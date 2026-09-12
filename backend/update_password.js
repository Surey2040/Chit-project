const db = require('sqlite3');
const bcrypt = require('bcrypt');

const database = new db.Database('database.sqlite');

bcrypt.hash('2204', 10).then(hash => {
    const sql = `UPDATE Users SET passwordHash = '${hash}', phone = '9342331980' WHERE username = 'admin'`;
    
    database.run(sql, function(err) {
        if (err) {
            console.error('Error updating user:', err);
        } else {
            console.log('Admin user successfully updated with PIN 2204 and phone 9342331980! Rows modified:', this.changes);
        }
        database.close();
    });
});
