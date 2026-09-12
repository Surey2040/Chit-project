/**
 * Notification Service
 * For MVP, this simulates sending WhatsApp messages and SMS by logging to the console.
 */

const sendWhatsAppMessage = (phone, text, mediaUrl = null) => {
    console.log(`\n=========================================`);
    console.log(`📱 [WHATSAPP API MOCK] Sending to: ${phone}`);
    console.log(`-----------------------------------------`);
    console.log(`${text}`);
    if (mediaUrl) {
        console.log(`\n[ATTACHMENT]: ${mediaUrl}`);
    }
    console.log(`=========================================\n`);
    
    // In production, you would call Twilio API or Meta Graph API here.
    return Promise.resolve(true);
};

const sendSMS = (phone, text) => {
    console.log(`\n=========================================`);
    console.log(`✉️ [SMS API MOCK] Sending to: ${phone}`);
    console.log(`-----------------------------------------`);
    console.log(`${text}`);
    console.log(`=========================================\n`);
    
    // In production, call AWS SNS or Twilio SMS here.
    return Promise.resolve(true);
};

module.exports = {
    sendWhatsAppMessage,
    sendSMS
};
