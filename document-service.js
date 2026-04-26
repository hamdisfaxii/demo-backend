/**
 * Service de Génération de Documents
 * PDF et Excel pour demandes de congés et rapports RH
 */

const PDFDocument = require("pdfkit");
const ExcelJS = require("exceljs");
const fs = require("fs");
const path = require("path");
const moment = require("moment");

class DocumentService {
  constructor() {
    this.docsDir = path.join(__dirname, "documents");
    if (!fs.existsSync(this.docsDir)) {
      fs.mkdirSync(this.docsDir, { recursive: true });
    }
  }

  /**
   * Générer attestation PDF pour une demande
   */
  generateDemandeAttestation(demande, user) {
    return new Promise((resolve, reject) => {
      try {
        const fileName = `demande_${demande.id}_${moment().format("YYYY-MM-DD")}.pdf`;
        const filePath = path.join(this.docsDir, fileName);

        const doc = new PDFDocument({
          size: "A4",
          margin: 50,
        });

        const stream = fs.createWriteStream(filePath);
        doc.pipe(stream);

        // Header
        doc.fontSize(24).font("Helvetica-Bold").text("DEMANDE DE CONGÉ", {
          align: "center",
        });
        doc.moveDown(0.5);
        doc.fontSize(10).text("Système de Gestion des Congés - Dolibarr", {
          align: "center",
        });
        doc.moveTo(50, doc.y).lineTo(550, doc.y).stroke();
        doc.moveDown(1);

        // Informations employé
        doc.fontSize(12).font("Helvetica-Bold").text("INFORMATIONS EMPLOYÉ");
        doc.fontSize(10).font("Helvetica");
        doc.text(`Nom: ${user.fullName}`, { indent: 20 });
        doc.text(`Email: ${user.email}`, { indent: 20 });
        doc.text(`Pays: ${user.pays || "N/A"}`, { indent: 20 });
        doc.text(`ID: ${user.id}`, { indent: 20 });
        doc.moveDown(0.5);

        // Informations demande
        doc.fontSize(12).font("Helvetica-Bold").text("DÉTAILS DE LA DEMANDE");
        doc.fontSize(10).font("Helvetica");
        doc.text(`Type de congé: ${demande.type_conge}`, { indent: 20 });
        doc.text(`Date de début: ${demande.date_debut}`, { indent: 20 });
        doc.text(`Date de fin: ${demande.date_fin}`, { indent: 20 });
        doc.text(`Nombre de jours: ${demande.nombre_jours || "N/A"}`, {
          indent: 20,
        });
        doc.text(`Raison: ${demande.raison || "Non spécifiée"}`, {
          indent: 20,
        });
        doc.text(`Statut: ${demande.statut}`, { indent: 20 });
        doc.moveDown(0.5);

        // Timeline des approbations
        doc.fontSize(12).font("Helvetica-Bold").text("TIMELINE D'APPROBATION");
        doc.fontSize(10).font("Helvetica");

        if (demande.manager_approval) {
          doc.text(`✓ Approuvé par Manager`, { indent: 20 });
          doc.text(`  Date: ${demande.manager_approval_date}`, {
            indent: 40,
          });
        } else {
          doc.text(`⏳ En attente d'approbation Manager`, { indent: 20 });
        }

        if (demande.rh_approval) {
          doc.text(`✓ Approuvé par RH`, { indent: 20 });
          doc.text(`  Date: ${demande.rh_approval_date}`, { indent: 40 });
        } else {
          doc.text(`⏳ En attente d'approbation RH`, { indent: 20 });
        }

        doc.moveDown(1);

        // Footer
        doc
          .fontSize(9)
          .text(
            `Document généré le ${moment().format(
              "DD/MM/YYYY HH:mm",
            )} | Demande ID: ${demande.id}`,
            {
              align: "center",
              opacity: 0.5,
            },
          );

        doc.end();

        stream.on("finish", () => {
          console.log(`📄 PDF généré: ${fileName}`);
          resolve({
            fileName,
            filePath,
            size: fs.statSync(filePath).size,
          });
        });

        stream.on("error", reject);
      } catch (error) {
        reject(error);
      }
    });
  }

  /**
   * Générer rapport Excel pour RH
   */
  async generateRHReport(demandes, filters = {}) {
    try {
      const workbook = new ExcelJS.Workbook();
      const worksheet = workbook.addWorksheet("Rapport Congés");

      // Headers
      worksheet.columns = [
        { header: "ID Demande", key: "id", width: 12 },
        { header: "Employé", key: "employee_name", width: 20 },
        { header: "Type de Congé", key: "type_conge", width: 15 },
        { header: "Date Début", key: "date_debut", width: 12 },
        { header: "Date Fin", key: "date_fin", width: 12 },
        { header: "Jours", key: "nombre_jours", width: 8 },
        { header: "Statut", key: "statut", width: 12 },
        { header: "Manager", key: "manager_approval", width: 10 },
        { header: "RH", key: "rh_approval", width: 10 },
        { header: "Pays", key: "pays", width: 8 },
        { header: "Date Création", key: "date_created", width: 12 },
      ];

      // Ajouter les données
      demandes.forEach((demande) => {
        worksheet.addRow({
          id: demande.id,
          employee_name: demande.employee_name,
          type_conge: demande.type_conge,
          date_debut: demande.date_debut,
          date_fin: demande.date_fin,
          nombre_jours: demande.nombre_jours,
          statut: demande.statut,
          manager_approval: demande.manager_approval ? "✓" : "✗",
          rh_approval: demande.rh_approval ? "✓" : "✗",
          pays: demande.pays,
          date_created: demande.date_created,
        });
      });

      // Formatting
      const headerRow = worksheet.getRow(1);
      headerRow.font = { bold: true, color: { argb: "FFFFFFFF" } };
      headerRow.fill = {
        type: "pattern",
        pattern: "solid",
        fgColor: { argb: "FF4472C4" },
      };

      const fileName = `rapport_conges_${moment().format("YYYY-MM-DD_HHmm")}.xlsx`;
      const filePath = path.join(this.docsDir, fileName);

      await workbook.xlsx.writeFile(filePath);

      console.log(`📊 Excel généré: ${fileName}`);

      return {
        fileName,
        filePath,
        size: fs.statSync(filePath).size,
        rowCount: demandes.length,
      };
    } catch (error) {
      console.error("Erreur generation Excel:", error);
      throw error;
    }
  }

  /**
   * Générer rapport statistiques RH (PDF)
   */
  generateRHStatisticsPDF(stats, dateRange = {}) {
    return new Promise((resolve, reject) => {
      try {
        const fileName = `rapport_stats_${moment().format("YYYY-MM-DD")}.pdf`;
        const filePath = path.join(this.docsDir, fileName);

        const doc = new PDFDocument({ size: "A4", margin: 50 });
        const stream = fs.createWriteStream(filePath);

        doc.pipe(stream);

        // Title
        doc
          .fontSize(20)
          .font("Helvetica-Bold")
          .text("RAPPORT RH - STATISTIQUES", {
            align: "center",
          });
        doc.moveDown(0.5);
        doc
          .fontSize(10)
          .font("Helvetica")
          .text(
            `Période: ${dateRange.start || "N/A"} à ${dateRange.end || "N/A"}`,
            {
              align: "center",
            },
          );
        doc.moveTo(50, doc.y).lineTo(550, doc.y).stroke();
        doc.moveDown(1);

        // Statistiques globales
        doc.fontSize(14).font("Helvetica-Bold").text("STATISTIQUES GLOBALES");
        doc.fontSize(11).font("Helvetica");
        doc.text(`Total demandes: ${stats.totalDemandes || 0}`, { indent: 20 });
        doc.text(`Approuvées: ${stats.approved || 0}`, { indent: 20 });
        doc.text(`Rejetées: ${stats.rejected || 0}`, { indent: 20 });
        doc.text(`En cours: ${stats.pending || 0}`, { indent: 20 });
        doc.moveDown(0.5);

        // Par pays
        if (stats.byCountry) {
          doc.fontSize(14).font("Helvetica-Bold").text("PAR PAYS");
          Object.keys(stats.byCountry).forEach((country) => {
            const countryData = stats.byCountry[country];
            doc.fontSize(11).text(`${country}:`, { indent: 20 });
            doc.text(`  - Total: ${countryData.total}`, { indent: 30 });
            doc.text(`  - Approuvées: ${countryData.approved}`, {
              indent: 30,
            });
          });
          doc.moveDown(0.5);
        }

        // Employés les plus absents
        if (stats.topAbsent) {
          doc
            .fontSize(14)
            .font("Helvetica-Bold")
            .text("EMPLOYÉS LES PLUS ABSENTS");
          stats.topAbsent.slice(0, 10).forEach((emp, idx) => {
            doc
              .fontSize(10)
              .text(`${idx + 1}. ${emp.name} - ${emp.days} jours`, {
                indent: 20,
              });
          });
        }

        doc.moveDown(1);
        doc
          .fontSize(9)
          .text(`Rapport généré le ${moment().format("DD/MM/YYYY HH:mm")}`, {
            align: "center",
            opacity: 0.5,
          });

        doc.end();

        stream.on("finish", () => {
          console.log(`📊 Rapport PDF généré: ${fileName}`);
          resolve({
            fileName,
            filePath,
            size: fs.statSync(filePath).size,
          });
        });

        stream.on("error", reject);
      } catch (error) {
        reject(error);
      }
    });
  }

  /**
   * Lister les documents générés
   */
  listDocuments() {
    const files = fs.readdirSync(this.docsDir);
    return files.map((file) => {
      const filePath = path.join(this.docsDir, file);
      const stats = fs.statSync(filePath);
      return {
        fileName: file,
        size: stats.size,
        created: stats.birthtime,
        modified: stats.mtime,
      };
    });
  }

  /**
   * Télécharger un document
   */
  getDocumentPath(fileName) {
    const filePath = path.join(this.docsDir, fileName);
    if (!fs.existsSync(filePath)) {
      throw new Error("Document not found");
    }
    return filePath;
  }

  /**
   * Supprimer un document
   */
  deleteDocument(fileName) {
    const filePath = path.join(this.docsDir, fileName);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
      console.log(`🗑️  Document supprimé: ${fileName}`);
      return true;
    }
    return false;
  }
}

module.exports = new DocumentService();
