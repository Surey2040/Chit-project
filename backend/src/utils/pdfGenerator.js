const PDFDocument = require('pdfkit');
const fs = require('fs');
const path = require('path');

/**
 * Generates a PDF receipt for a payment
 * @param {Object} payment - Payment model instance
 * @param {Object} member - User model instance (the member who paid)
 * @returns {Promise<String>} - Returns the URL path of the generated PDF
 */
function generateReceiptPdf(payment, member) {
    return new Promise((resolve, reject) => {
        try {
            const doc = new PDFDocument({ margin: 50 });
            const fileName = `receipt_${payment.receiptNo}.pdf`;
            const receiptsDir = path.join(__dirname, '..', '..', 'public', 'receipts');
            const publicPath = path.join(receiptsDir, fileName);
            const relativeUrl = `/receipts/${fileName}`;

            fs.mkdirSync(receiptsDir, { recursive: true });
            const writeStream = fs.createWriteStream(publicPath);
            doc.pipe(writeStream);

            // Header
            doc.fontSize(20).font('Helvetica-Bold').text('JOTHI VEL CHITS', { align: 'center' });
            doc.moveDown();
            doc.fontSize(14).text('Payment Receipt', { align: 'center' });
            doc.moveDown(2);

            // Receipt Details
            doc.fontSize(12).font('Helvetica');
            doc.text(`Receipt No: ${payment.receiptNo}`);
            doc.text(`Date: ${new Date(payment.paidAt).toLocaleDateString()}`);
            doc.moveDown();

            doc.text(`Member Name: ${member.name}`);
            doc.text(`Member Phone: ${member.phone}`);
            doc.moveDown();

            doc.text(`Payment Mode: ${payment.mode}`);
            if (payment.referenceNo) {
                doc.text(`Reference No: ${payment.referenceNo}`);
            }
            doc.moveDown();

            // Amount
            doc.font('Helvetica-Bold').fontSize(14);
            doc.text(`Amount Paid: Rs. ${(payment.amountPaid / 100).toFixed(2)}`);
            doc.moveDown();
            
            if (payment.status === 'PARTIAL') {
                doc.fontSize(12).font('Helvetica').text(`Note: This is a partial payment. Remaining due: Rs. ${((payment.amountDue - payment.amountPaid)/100).toFixed(2)}`);
            }

            // Footer
            doc.moveDown(4);
            doc.fontSize(10).font('Helvetica-Oblique').text('This is a computer-generated receipt and does not require a signature.', { align: 'center' });

            doc.end();

            writeStream.on('finish', () => {
                resolve(relativeUrl);
            });
            
            writeStream.on('error', (err) => {
                reject(err);
            });

        } catch (error) {
            reject(error);
        }
    });
}

module.exports = {
    generateReceiptPdf
};
