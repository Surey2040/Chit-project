import csv
import random
from datetime import datetime, timedelta

names = ['Ramesh', 'Suresh', 'Murugan', 'Karthik', 'Vijay', 'Ajith', 'Surya', 'Kamal', 'Rajini', 'Dhanush', 'Siva', 'Mani', 'Gopal', 'Krishna', 'Bala', 'Arun', 'Prakash', 'Saravanan', 'Ganesh', 'Vignesh']
groups = ['5L/20M', '10L/20M', '1L/10M']
statuses = ['PAID', 'PARTIAL', 'DUE', 'OVERDUE']
modes = ['CASH', 'UPI', 'BANK_TRANSFER', '']

with open('sample_payments_data.csv', 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['Member Name', 'Phone', 'Chit Group', 'Installment No', 'Base Amount', 'Amount Due', 'Amount Paid', 'Payment Status', 'Payment Mode', 'Due Date'])
    
    for i in range(100):
        name = random.choice(names) + ' ' + str(random.randint(1, 100))
        phone = '9' + ''.join([str(random.randint(0, 9)) for _ in range(9)])
        group = random.choice(groups)
        inst_no = random.randint(1, 20)
        
        base_amt = 25000 if group == '5L/20M' else (50000 if group == '10L/20M' else 10000)
        kasaru = random.randint(1000, 5000)
        amt_due = base_amt - kasaru
        
        status = random.choices(statuses, weights=[0.4, 0.2, 0.2, 0.2])[0]
        
        if status == 'PAID':
            amt_paid = amt_due
            mode = random.choice(modes[:-1])
        elif status == 'PARTIAL':
            amt_paid = int(amt_due / 2)
            mode = random.choice(modes[:-1])
        elif status in ['DUE', 'OVERDUE']:
            amt_paid = 0
            mode = ''
            
        due_date = datetime.now() + timedelta(days=random.randint(-30, 30))
        due_date_str = due_date.strftime('%Y-%m-%d')
        
        writer.writerow([name, phone, group, inst_no, base_amt, amt_due, amt_paid, status, mode, due_date_str])
print('CSV generated successfully.')
