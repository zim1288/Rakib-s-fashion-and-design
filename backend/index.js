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
mongoose.connect(process.env.MONGODB_URI, { dbName: 'Silk_and_fashion' })
  .then(() => console.log('Successfully connected to Silk_and_fashion MongoDB!'))
  .catch(err => {
      console.error('Could not connect to MongoDB:', err);
      process.exit(1);
  });

// Health check route for testing backend status
app.get('/health', (req, res) => {
    res.status(200).json({ status: 'ok', database: mongoose.connection.readyState === 1 ? 'connected' : 'disconnected' });
});

// Define Schemas for the requested structure
const UserSchema = new mongoose.Schema({
    email: { type: String, required: true, unique: true },
    password: { type: String, required: true }
}, { collection: 'user' });

const UserLogSchema = new mongoose.Schema({
    email: { type: String, required: true },
    id: { type: Number, required: true },
    // Inventory fields
    modelName: String,
    brandCategory: String,
    unitPrice: Number,
    pieceCount: Number,
    imageUrl: String,
    // Production fields
    quantity: Number,
    estimatedCompletionDate: String,
    status: String,
    // Transaction fields
    type: String, // HOUSE, PRODUCTION, PURCHASE_IN, SELL_OUT
    totalAmount: Number,
    timestamp: Number,
    dateString: String
}, { collection: 'user-log', strict: false });

// Create Models
const User = mongoose.model('User', UserSchema);
const UserLog = mongoose.model('UserLog', UserLogSchema);

// --- Inventory Routes ---
app.get('/v1/inventory', async (req, res) => {
    try {
        const items = await UserLog.find({ dataType: 'inventory' }, '-__v');
        res.json(items);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/v1/inventory', async (req, res) => {
    try {
        const items = req.body;
        if (!Array.isArray(items)) return res.status(400).json({ error: 'Body must be an array' });

        const email = req.header('X-User-Email') || 'anonymous';
        const bulkOps = items.map(item => ({
            updateOne: {
                filter: { id: item.id, dataType: 'inventory' },
                update: { $set: { ...item, email, dataType: 'inventory' } },
                upsert: true
            }
        }));
        if (bulkOps.length > 0) await UserLog.bulkWrite(bulkOps);
        res.status(200).send();
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/v1/inventory/item', async (req, res) => {
    try {
        const item = req.body;
        const email = req.header('X-User-Email') || 'anonymous';
        await UserLog.findOneAndUpdate({ id: item.id, dataType: 'inventory' }, { ...item, email, dataType: 'inventory' }, { upsert: true });
        res.status(200).send();
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/v1/inventory/item/:id', async (req, res) => {
    try {
        await UserLog.findOneAndDelete({ id: parseInt(req.params.id), dataType: 'inventory' });
        res.status(200).send();
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// --- Production Routes ---
app.get('/v1/production', async (req, res) => {
    try {
        const items = await UserLog.find({ dataType: 'production' }, '-__v');
        res.json(items);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/v1/production', async (req, res) => {
    try {
        const items = req.body;
        if (!Array.isArray(items)) return res.status(400).json({ error: 'Body must be an array' });

        const email = req.header('X-User-Email') || 'anonymous';
        const bulkOps = items.map(item => ({
            updateOne: {
                filter: { id: item.id, dataType: 'production' },
                update: { $set: { ...item, email, dataType: 'production' } },
                upsert: true
            }
        }));
        if (bulkOps.length > 0) await UserLog.bulkWrite(bulkOps);
        res.status(200).send();
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// --- Transaction Routes ---
app.get('/v1/transactions', async (req, res) => {
    try {
        const logs = await UserLog.find({ dataType: 'transaction' }, '-__v');
        res.json(logs);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/v1/transactions', async (req, res) => {
    try {
        const log = req.body;
        const email = req.header('X-User-Email') || 'anonymous';

        // Check if an array was sent by mistake, though endpoint expects single object
        if (Array.isArray(log)) {
             const bulkOps = log.map(item => ({
                 updateOne: {
                     filter: { id: item.id, dataType: 'transaction' },
                     update: { $set: { ...item, email, dataType: 'transaction' } },
                     upsert: true
                 }
             }));
             if (bulkOps.length > 0) await UserLog.bulkWrite(bulkOps);
        } else {
             await UserLog.findOneAndUpdate({ id: log.id, dataType: 'transaction' }, { ...log, email, dataType: 'transaction' }, { upsert: true });
        }
        res.status(200).send();
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/v1/auth/register', async (req, res) => {
    try {
        const { email, password } = req.body;
        if (!email || !password) return res.status(400).json({ error: 'Email and password required' });

        await User.findOneAndUpdate(
            { email },
            { email, password },
            { upsert: true }
        );
        res.status(200).json({ status: 'ok' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// --- Email Verification Router ---

app.post('/v1/auth/send-verification', async (req, res) => {
    const { email, code } = req.body;
    if (!email || !code) {
        return res.status(400).json({ error: 'Email and code are required' });
    }

    try {
        const emailJsData = {
            service_id: 'service_gifq25t',
            template_id: 'template_fxkonbp',
            user_id: 'S-Rf-rtEkTugqgxM4',
            template_params: {
                to_email: email,
                email: email,
                code: code,
                otp: code,
                message: code
            }
        };

        const response = await fetch('https://api.emailjs.com/api/v1.0/email/send', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(emailJsData)
        });

        if (response.ok) {
            console.log(`Verification email sent to ${email} successfully via EmailJS!`);
            res.status(200).json({ status: 'ok' });
        } else {
            const errorText = await response.text();
            console.error('EmailJS Error:', errorText);
            res.status(500).json({ error: 'Failed to send verification email via EmailJS: ' + errorText });
        }
    } catch (err) {
        console.error('Error sending verification email via EmailJS:', err);
        res.status(500).json({ error: 'Failed to send verification email: ' + err.message });
    }
});

// Start the Server
const PORT = process.env.PORT || 5000;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on port ${PORT}`);
});
