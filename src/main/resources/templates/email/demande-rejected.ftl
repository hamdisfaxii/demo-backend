<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
        .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .header { background-color: #e74c3c; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { padding: 20px; }
        .footer { background-color: #ecf0f1; padding: 15px; text-align: center; font-size: 12px; color: #7f8c8d; border-radius: 0 0 8px 8px; }
        .info-box { background-color: #fadbd8; padding: 15px; border-radius: 4px; margin: 15px 0; border-left: 4px solid #e74c3c; }
        .info-box strong { color: #e74c3c; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>❌ Demande Rejetée</h1>
        </div>
        
        <div class="content">
            <p>Bonjour <strong>${userName}</strong>,</p>
            
            <p>Nous vous informons que votre demande de congé a été <strong>REJETÉE</strong>.</p>
            
            <div class="info-box">
                <p><strong>Détails de votre demande :</strong></p>
                <ul>
                    <li><strong>Type de congé :</strong> ${typeConge}</li>
                    <li><strong>Date de début :</strong> ${dateDebut}</li>
                    <li><strong>Date de fin :</strong> ${dateFin}</li>
                    <li><strong>Motif du rejet :</strong> ${reason}</li>
                </ul>
            </div>
            
            <p>Si vous avez des questions ou souhaitez contester cette décision, veuillez contacter le département RH.</p>
            
            <p>Cordialement,<br>
            <strong>L'équipe Gestion des Congés</strong></p>
        </div>
        
        <div class="footer">
            <p>Cet email a été généré automatiquement. Veuillez ne pas répondre directement.</p>
        </div>
    </div>
</body>
</html>
