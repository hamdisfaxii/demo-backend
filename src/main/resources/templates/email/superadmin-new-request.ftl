<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
        .container { max-width: 640px; margin: 20px auto; background-color: white; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.08); overflow: hidden; }
        .header { background-color: #0f172a; color: white; padding: 18px 20px; }
        .header h1 { margin: 0; font-size: 18px; }
        .content { padding: 18px 20px; color: #0f172a; }
        .muted { color: #64748b; font-size: 12px; }
        .box { background: #f1f5f9; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px 14px; margin: 14px 0; }
        .box ul { margin: 10px 0 0 18px; padding: 0; }
        .btn { display: inline-block; padding: 10px 14px; background-color: #2563eb; color: white !important; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 13px; }
        .footer { background-color: #f8fafc; padding: 12px 20px; font-size: 12px; color: #64748b; }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <h1>Nouvelle demande de congé</h1>
    </div>
    <div class="content">
        <p>Une nouvelle demande a été créée.</p>

        <div class="box">
            <div><strong>Collaborateur :</strong> ${requesterName} <span class="muted">(${requesterEmail})</span></div>
            <ul>
                <li><strong>Type :</strong> ${typeConge}</li>
                <li><strong>Début :</strong> ${dateDebut}</li>
                <li><strong>Fin :</strong> ${dateFin}</li>
                <li><strong>Durée :</strong> ${nombreJours}</li>
                <li><strong>Commentaire :</strong> ${motif}</li>
                <li><strong>Approuvé par :</strong> ${approvedBy}</li>
            </ul>
        </div>

        <p style="margin-top: 14px;">
            <a class="btn" href="${appUrl}/rh/requests/${demandeId}">Ouvrir la demande</a>
        </p>
        <p class="muted">Lien : ${appUrl}/rh/requests/${demandeId}</p>
    </div>
    <div class="footer">
        Email automatique — ne pas répondre.
    </div>
</div>
</body>
</html>

