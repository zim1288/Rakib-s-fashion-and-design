require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

// Check environment variables first
if (!process.env.MONGODB_URI) {
    console.error('FATAL ERROR: MONGODB_URI is not defined in the .env file.');
    process.exit(1);
}

// Connect to MongoDB using modern driver framework
mongoose.connect(process.env.MONGODB_URI)
  .then(() => console.log('Successfully connected to TallyApp MongoDB Cluster!'))
  .catch(err => {
      console.error('Could not connect to MongoDB:', err);
      process.exit(1);
  });

// Health check route for testing backend status
app.get('/health', (req, res) => {
    res.status(200).json({ status: 'ok', database: mongoose.connection.readyState === 1 ? 'connected' : 'disconnected' });
});

// Define Schemas mirroring the app's models precisely
const InventorySchema = new mongoose.Schema({
    id: { type: Number, required: true, unique: true },
    modelName: String,
    brandCategory: String,
    unitPrice: Number,
    pieceCount: Number,
    imageUrl: String
});

const ProductionSchema = new mongoose.Schema({
    id: { type: Number, required: true, unique: true },
    modelName: String,
    quantity: Number,
    estimatedCompletionDate: String,
    status: String
});

const TransactionSchema = new mongoose.Schema({
    id: { type: Number, required: true, unique: true },
    type: { type: String }, // HOUSE, PRODUCTION, PURCHASE_IN, SELL_OUT
    modelName: String,
    quantity: Number,
    unitPrice: Number,
    totalAmount: Number,
    timestamp: Number,
    dateString: String
});

// Create Models
const InventoryItem = mongoose.model('InventoryItem', InventorySchema);
const ProductionItem = mongoose.model('ProductionItem', ProductionSchema);
const TransactionLog = mongoose.model('TransactionLog', TransactionSchema);

// --- Inventory Routes ---
app.get('/v1/inventory', async (req, res) => {
    try {
        const items = await InventoryItem.find({}, '-_id -__v');
        res.json(items);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/v1/inventory', async (req, res) => {
    try {
        const items = req.body;
        if (!Array.isArray(items)) return res.status(400).json({ error: 'Body must be an array' });
        
        const bulkOps = items.map(item => ({
            updateOne: { filter: { id: item.id }, update: { $set: item }, upsert: true }
        }));
        if (bulkOps.length > 0) await InventoryItem.bulkWrite(bulkOps);
        res.status(200).send();
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/v1/inventory/item', async (req, res) => {
    try {
        const item = req.body;
        await InventoryItem.findOneAndUpdate({ id: item.id }, item, { upsert: true });
        res.status(200).send();
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/v1/inventory/item/:id', async (req, res) => {
    try {
        await InventoryItem.findOneAndDelete({ id: parseInt(req.params.id) });
        res.status(200).send();
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// --- Production Routes ---
app.get('/v1/production', async (req, res) => {
    try {
        const items = await ProductionItem.find({}, '-_id -__v');
        res.json(items);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/v1/production', async (req, res) => {
    try {
        const items = req.body;
        if (!Array.isArray(items)) return res.status(400).json({ error: 'Body must be an array' });

        const bulkOps = items.map(item => ({
            updateOne: { filter: { id: item.id }, update: { $set: item }, upsert: true }
        }));
        if (bulkOps.length > 0) await ProductionItem.bulkWrite(bulkOps);
        res.status(200).send();
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// --- Transaction Routes ---
app.get('/v1/transactions', async (req, res) => {
    try {
        const logs = await TransactionLog.find({}, '-_id -__v');
        res.json(logs);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/v1/transactions', async (req, res) => {
    try {
        const log = req.body;
        // Check if an array was sent by mistake, though endpoint expects single object
        if (Array.isArray(log)) {
             const bulkOps = log.map(item => ({
                 updateOne: { filter: { id: item.id }, update: { $set: item }, upsert: true }
             }));
             if (bulkOps.length > 0) await TransactionLog.bulkWrite(bulkOps);
        } else {
             await TransactionLog.findOneAndUpdate({ id: log.id }, log, { upsert: true });
        }
        res.status(200).send();
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// --- Email Verification Router ---
const nodemailer = require('nodemailer');

const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS
    }
});

app.post('/v1/auth/send-verification', async (req, res) => {
    const { email, code } = req.body;
    if (!email || !code) {
        return res.status(400).json({ error: 'Email and code are required' });
    }

    const mailOptions = {
        from: `"Rakib Silk & Fashion" <${process.env.SMTP_USER}>`,
        to: email,
        subject: 'Your Confirmation Code',
        text: `Hello!\n\nHere is the one-time code to confirm your personal email address:\n\n${code}\n\nIf you didn't request the code, you can ignore this email.\n\nKind regards,\nRakib Silk & Fashion Team`,
        html: `
            <div style="font-family: Arial, sans-serif; padding: 20px; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px;">
                <p>Hello!</p>
                <p>Here is the one-time code to confirm your personal email address:</p>
                <h1 style="font-size: 36px; font-weight: bold; color: #800020; letter-spacing: 2px; margin: 20px 0;">${code}</h1>
                <p>If you didn't request the code, you can ignore this email.</p>
                <br/>
                <p>Kind regards,</p>
                <p><strong>The Rakib Silk & Fashion team</strong></p>
            </div>
        `
    };

    try {
        await transporter.sendMail(mailOptions);
        console.log(`Verification email sent to ${email} successfully!`);
        res.status(200).json({ status: 'ok' });
    } catch (err) {
        console.error('Error sending verification email:', err);
        res.status(500).json({ error: 'Failed to send verification email: ' + err.message });
    }
});

// Start the Server
const PORT = process.env.PORT || 5000;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on port ${PORT}`);
});
