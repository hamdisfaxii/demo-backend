#!/usr/bin/env node

/**
 * Email SMTP Test Script
 * Test Gmail SMTP configuration without sending to users
 * 
 * Usage:
 *   node test-email-config.js
 *   npm run test:email
 */

const path = require("path");
const nodemailer = require("nodemailer");
require("dotenv").config({ path: path.join(__dirname, ".env") });

const colors = {
  reset: "\x1b[0m",
  green: "\x1b[32m",
  red: "\x1b[31m",
  yellow: "\x1b[33m",
  blue: "\x1b[34m",
  cyan: "\x1b[36m",
};

function log(type, message) {
  const icons = {
    success: `${colors.green}✅${colors.reset}`,
    error: `${colors.red}❌${colors.reset}`,
    info: `${colors.blue}ℹ️${colors.reset}`,
    warning: `${colors.yellow}⚠️${colors.reset}`,
    test: `${colors.cyan}🧪${colors.reset}`,
  };
  console.log(`${icons[type]} ${message}`);
}

async function testSmtpConfig() {
  log("info", "Starting SMTP Configuration Test...\n");

  // 1. Check environment variables
  log("test", "Checking environment variables...");
  const requiredVars = [
    "MAIL_HOST",
    "MAIL_PORT",
    "MAIL_USERNAME",
    "MAIL_PASSWORD",
  ];
  let allVarsPresent = true;

  for (const varName of requiredVars) {
    const value = process.env[varName];
    if (!value) {
      log("error", `Missing: ${varName}`);
      allVarsPresent = false;
    } else {
      const masked =
        varName === "MAIL_PASSWORD"
          ? value.substring(0, 3) + "***"
          : value.substring(0, 3) + "***";
      log("success", `Found: ${varName} = ${masked}`);
    }
  }

  if (!allVarsPresent) {
    log("error", "Missing environment variables. Create .env file.");
    process.exit(1);
  }

  log("success", "All environment variables found!\n");

  // 2. Create transporter
  log("test", "Creating email transporter...");
  const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: {
      user: process.env.MAIL_USERNAME,
      pass: process.env.MAIL_PASSWORD,
    },
  });

  log("success", "Transporter created.\n");

  // 3. Verify SMTP connection
  log("test", "Verifying SMTP connection...");
  try {
    await transporter.verify();
    log("success", "SMTP Connection Verified! ✓\n");
  } catch (error) {
    log("error", `SMTP Connection Failed: ${error.message}`);

    // Common errors
    if (error.message.includes("535")) {
      log("warning", "Error 535: Invalid credentials");
      log("info", "→ Check Email and App Password");
      log("info", "→ Ensure 2FA is enabled on Gmail");
    } else if (error.message.includes("timeout")) {
      log("warning", "Connection Timeout");
      log("info", "→ Check MAIL_HOST and MAIL_PORT");
      log("info", "→ Ensure internet connection is stable");
    }

    process.exit(1);
  }

  // 4. Send test email
  log("test", "Sending test email...");
  const mailOptions = {
    from: `${process.env.MAIL_FROM_NAME || "Gestion des Congés"} <${process.env.MAIL_USERNAME}>`,
    to: process.env.MAIL_USERNAME, // Send to self for testing
    subject: "[TEST] SMTP Configuration - Gestion des Congés",
    html: `
      <div style="font-family: Arial, sans-serif; color: #333;">
        <h2 style="color: #4CAF50;">✅ Configuration SMTP Réussie</h2>
        <p>Cet email confirme que votre configuration SMTP est opérationnelle.</p>
        
        <div style="background: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0;">
          <h3>Détails de Configuration:</h3>
          <ul>
            <li><strong>Service:</strong> Gmail SMTP</li>
            <li><strong>Host:</strong> ${process.env.MAIL_HOST}</li>
            <li><strong>Port:</strong> ${process.env.MAIL_PORT}</li>
            <li><strong>Auth:</strong> Activée</li>
            <li><strong>TLS:</strong> ${process.env.MAIL_PORT === "587" ? "Oui (STARTTLS)" : "Non"}</li>
          </ul>
        </div>
        
        <p style="color: #666; font-size: 12px;">
          Email généré: ${new Date().toLocaleString()}
        </p>
      </div>
    `,
  };

  try {
    const info = await transporter.sendMail(mailOptions);
    log("success", "Test email sent successfully!");
    log("info", `Message ID: ${info.messageId}`);
    log("info", `Response: ${info.response}`);
    log("success", "\n🎉 All tests passed! SMTP is ready for production.\n");
  } catch (error) {
    log("error", `Failed to send email: ${error.message}`);
    process.exit(1);
  }
}

// Run tests
testSmtpConfig().catch((error) => {
  log("error", `Unexpected error: ${error.message}`);
  process.exit(1);
});
