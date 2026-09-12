const { v4: uuidv4 } = require('uuid');
const db = require('sqlite3');
const bcrypt = require('bcrypt');

const database = new db.Database('database.sqlite');

bcrypt.hash('2204', 10).then(hash => {
    const id = uuidv4();
    const sql = `INSERT INTO Users (id, username, phone, passwordHash, role, isActive, createdAt, updatedAt) VALUES ('${id}', 'admin', '9342331980', '${hash}', 'ADMIN', 1, datetime('now'), datetime('now'))`;
    
    database.run(sql, err => {
        if (err) {
            console.error('Error inserting user:', err);
        } else {
            console.log('Test User successfully inserted!');
        }
        database.close();
    });
});
