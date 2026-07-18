require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const multer = require('multer');
const { CloudinaryStorage } = require('multer-storage-cloudinary');
const cloudinary = require('cloudinary').v2;

const app = express();
app.use(cors());
app.use(express.json());

// Cloudinary Configuration
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET
});

const storage = new CloudinaryStorage({
  cloudinary: cloudinary,
  params: {
    folder: 'tally-app',
    allowedFormats: ['jpg', 'png', 'jpeg', 'webp'],
  },
});

const upload = multer({ storage: storage });

// Check environment variables first
if (!process.env.MONGODB_URI) {
    console.error('WARNING: MONGODB_URI is not defined in the .env file. Database connection skipped.');
} else {
    // Connect to MongoDB using modern driver framework
    mongoose.connect(process.env.MONGODB_URI, { dbName: 'Silk_and_fashion' })
      .then(() => console.log('Successfully connected to Silk_and_fashion MongoDB!'))
      .catch(err => {
          console.error('Could not connect to MongoDB:', err);
      });
}

// Health check route for testing backend status
app.get('/health', (req, res) => {
    res.status(200).json({ status: 'ok', database: mongoose.connection.readyState === 1 ? 'connected' : 'disconnected' });
});

// Root route for preview
app.get('/', (req, res) => {
    res.send(`
        <html>
            <head><title>Rakib Silk & Fashion API</title></head>
            <body style="font-family: sans-serif; padding: 2rem; text-align: center;">
                <h1>Rakib Silk & Fashion Backend API is Running</h1>
                <p>This is the backend server for the Android application.</p>
                <p>Status: <a href="/health">/health</a></p>
                <hr style="margin-top: 2rem; border: none; border-top: 1px solid #ccc;" />
                <p style="color: #666; font-size: 0.9em;">Note: The Android app UI cannot be previewed in this web-based iframe. Please use the codebase for your development.</p>
            </body>
        </html>
    `);
});

// --- Upload Route ---
app.post('/v1/upload', upload.single('image'), (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'No image provided' });
        }
        res.json({ imageUrl: req.file.path });
    } catch (err) {
        res.status(500).json({ error: 'Error uploading to Cloudinary: ' + err.message });
    }
});

// --- Delete Image Route ---
app.post('/v1/delete-image', async (req, res) => {
    try {
        const { imageUrl } = req.body;
        if (!imageUrl) return res.status(400).json({ error: 'No image URL provided' });

        const parts = imageUrl.split('/');
        const folderIndex = parts.indexOf('tally-app');
        if (folderIndex !== -1) {
            const publicIdWithExt = parts.slice(folderIndex).join('/');
            const publicId = publicIdWithExt.substring(0, publicIdWithExt.lastIndexOf('.'));
            await cloudinary.uploader.destroy(publicId);
        }
        res.status(200).send();
    } catch (err) {
        res.status(500).json({ error: 'Error deleting from Cloudinary: ' + err.message });
    }
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
        const email = req.header("X-User-Email");
        let filter = { dataType: "transaction" };
        if (email && email !== "anonymous") {
            filter.email = email;
        }
        const logs = await UserLog.find(filter, "-__v");
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
                     filter: { id: item.id, email: email, dataType: 'transaction' },
                     update: { $set: { ...item, email, dataType: 'transaction' } },
                     upsert: true
                 }
             }));
             if (bulkOps.length > 0) await UserLog.bulkWrite(bulkOps);
        } else {
             await UserLog.findOneAndUpdate({ id: log.id, email: email, dataType: 'transaction' }, { $set: { ...log, email, dataType: 'transaction' } }, { upsert: true });
        }
        res.status(200).send();
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});
app.post("/v1/customers/update", async (req, res) => {
    try {
        const { oldName, oldNumber, newName, newNumber } = req.body;
        const email = req.header("X-User-Email");
        if (!email) return res.status(401).send("Unauthorized");

        let updateFilter = { type: "SALE", email: email, dataType: "transaction" };
        if (oldNumber && oldNumber.trim() !== "") {
            updateFilter.customerNumber = oldNumber.trim();
        } else {
            updateFilter.$or = [
                { customerNumber: { $exists: false } },
                { customerNumber: "" },
                { customerNumber: null }
            ];
            updateFilter.customerName = { $regex: new RegExp("^" + (oldName || "").trim() + "$", "i") };
        }

        await UserLog.updateMany(updateFilter, { $set: { customerName: newName, customerNumber: newNumber } });
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

app.post('/v1/auth/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        if (!email || !password) return res.status(400).json({ error: 'Email and password required' });

        const user = await User.findOne({ email });
        if (!user) return res.status(404).json({ error: 'User not found' });

        if (user.password !== password) return res.status(401).json({ error: 'Invalid password' });

        res.status(200).json({ status: 'ok', email: user.email });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/v1/auth/check', async (req, res) => {
    try {
        const { email } = req.body;
        if (!email) return res.status(400).json({ error: 'Email required' });
        const user = await User.findOne({ email });
        if (!user) return res.status(404).json({ error: 'User not found' });
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

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return res.status(400).json({ error: 'Invalid email format' });
    }

    try {
        const emailJsData = {
            service_id: 'service_gifq25t',
            template_id: 'template_fxkonbp',
            user_id: 'S-Rf-rtEkTugqgxM4',
            template_params: {
                to_email: email,
                email: email,
                purpose: 'Silk & Fashion account verification',
                passcode: code,
                time: new Date(Date.now() + 15 * 60000).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
            }
        };

        // Add Private Key if it's available in the environment
        if (process.env.EMAILJS_PRIVATE_KEY) {
            emailJsData.accessToken = process.env.EMAILJS_PRIVATE_KEY;
        }

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
