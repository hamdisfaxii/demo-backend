<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
        .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .header { background-color: #e67e22; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { padding: 20px; }
        .footer { background-color: #ecf0f1; padding: 15px; text-align: center; font-size: 12px; color: #7f8c8d; border-radius: 0 0 8px 8px; }
        .alert-box { background-color: #fdeaa8; padding: 15px; border-radius: 4px; margin: 15px 0; border-left: 4px solid #e67e22; }
        .alert-box strong { color: #e67e22; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>⚠️ Alerte Solde de Congés Faible</h1>
        </div>
        
        <div class="content">
            <p>Bonjour <strong>${userName}</strong>,</p>
            
            <p>Nous vous informons que votre solde de congés payés est actuellement très faible.</p>
            
            <div class="alert-box">
                <p><strong>Jours restants :</strong> <span style="font-size: 24px; color: #e67e22;">${remainingDays}</span> jour(s)</p>
            </div>
            
            <p>Pensez à planifier vos congés avant la fin de l'année civile, car les jours non utilisés ne peuvent pas être reportés (selon les règles de votre entreprise).</p>
            
            <p>Pour des informations supplémentaires, veuillez contacter le département RH.</p>
            
            <p>Cordialement,<br>
            <strong>L'équipe Gestion des Congés</strong></p>
        </div>
        
        <div class="footer">
            <p>Cet email a été généré automatiquement. Veuillez ne pas répondre directement.</p>
        </div>
    </div>
</body>
</html>
