<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
        .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .header { background-color: #27ae60; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { padding: 20px; }
        .footer { background-color: #ecf0f1; padding: 15px; text-align: center; font-size: 12px; color: #7f8c8d; border-radius: 0 0 8px 8px; }
        .info-box { background-color: #d5f4e6; padding: 15px; border-radius: 4px; margin: 15px 0; border-left: 4px solid #27ae60; }
        .info-box strong { color: #27ae60; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>✅ Demande Approuvée</h1>
        </div>
        
        <div class="content">
            <p>Bonjour <strong>${userName}</strong>,</p>
            
            <p>Nous sommes heureux de vous informer que votre demande de congé a été <strong>APPROUVÉE</strong> par <strong>${approverName}</strong>.</p>
            
            <div class="info-box">
                <p><strong>Détails de votre demande :</strong></p>
                <ul>
                    <li><strong>Type de congé :</strong> ${typeConge}</li>
                    <li><strong>Date de début :</strong> ${dateDebut}</li>
                    <li><strong>Date de fin :</strong> ${dateFin}</li>
                    <li><strong>Nombre de jours :</strong> ${nombreJours}</li>
                    <li><strong>Approuvé par :</strong> ${approverName}</li>
                </ul>
            </div>
            
            <p>Vous pouvez maintenant profiter de vos congés en toute sérénité.</p>
            
            <p>Cordialement,<br>
            <strong>L'équipe Gestion des Congés</strong></p>
        </div>
        
        <div class="footer">
            <p>Cet email a été généré automatiquement. Veuillez ne pas répondre directement.</p>
        </div>
    </div>
</body>
</html>
