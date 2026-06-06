import os
import csv
import subprocess

# Paths
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__))) # Backend folder
PROJECT_ROOT = os.path.dirname(BASE_DIR)
CSV_DIR = os.path.join(PROJECT_ROOT, "config", "src", "main", "resources", "config", "csv")
OUTPUT_SQL = os.path.join(BASE_DIR, "scratch", "import_data.sql")

print(f"Project root: {PROJECT_ROOT}")
print(f"CSV directory: {CSV_DIR}")
print(f"Output SQL file: {OUTPUT_SQL}")

def to_sql_val(val, val_type='string'):
    if val is None or val.strip() == '':
        return 'NULL'
    val = val.strip()
    if val_type == 'string':
        escaped = val.replace("'", "''")
        return f"'{escaped}'"
    elif val_type == 'int':
        return str(int(val))
    elif val_type == 'float':
        return str(float(val))
    elif val_type == 'boolean':
        return 'true' if val.lower() in ('true', '1', 'y', 'yes') else 'false'
    elif val_type == 'datetime':
        escaped = val.replace("'", "''")
        return f"'{escaped}'"
    return f"'{val}'"

def main():
    sql_statements = []

    # 1. Truncate tables and reset identities
    sql_statements.append("-- Disable foreign key checks is handled by TRUNCATE CASCADE in Postgres")
    sql_statements.append("TRUNCATE TABLE auth.users RESTART IDENTITY CASCADE;")
    sql_statements.append("TRUNCATE TABLE auth.user_preferences RESTART IDENTITY CASCADE;")
    sql_statements.append("TRUNCATE TABLE auth.refresh_tokens RESTART IDENTITY CASCADE;")
    sql_statements.append("TRUNCATE TABLE wallet.categories RESTART IDENTITY CASCADE;")
    sql_statements.append("TRUNCATE TABLE wallet.wallets RESTART IDENTITY CASCADE;")
    sql_statements.append("TRUNCATE TABLE wallet.transactions RESTART IDENTITY CASCADE;")
    sql_statements.append("TRUNCATE TABLE transaction.transactions RESTART IDENTITY CASCADE;")
    sql_statements.append("TRUNCATE TABLE reporting.monthly_summaries RESTART IDENTITY CASCADE;")
    sql_statements.append("TRUNCATE TABLE reporting.budgets RESTART IDENTITY CASCADE;")
    sql_statements.append("TRUNCATE TABLE reporting.reports RESTART IDENTITY CASCADE;")
    sql_statements.append("")

    # 2. Parse users and passwords
    user_passwords = {}
    user_csv_path = os.path.join(CSV_DIR, "user.csv")
    if os.path.exists(user_csv_path):
        with open(user_csv_path, mode='r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                user_passwords[row['username']] = row['password']

    users_csv_path = os.path.join(CSV_DIR, "users.csv")
    sql_statements.append("-- Populate auth.users")
    with open(users_csv_path, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            u_id = row['user_id']
            username = row['username']
            email = row['email']
            phone = row['phone']
            avatar = row['avatar_url']
            is_active = row['is_active']
            created = row['created_date']
            
            # Default to standard BCrypt hash of 'secret' if CSV has dots or is empty
            hashed_pass = user_passwords.get(username, '')
            if not hashed_pass or '$2a$10$...' in hashed_pass or len(hashed_pass) < 20:
                hashed_pass = "$2a$10$X/Vl.K/JmI21t0T3YQ/wV.Q4V/24I0y70y54P5gO.xG0M7B/0K1Q." # 'secret'

            sql = (f"INSERT INTO auth.users (id, username, email, phone_number, avatar_url, "
                   f"hashed_password, role, is_active, created_at, updated_at) VALUES ("
                   f"{to_sql_val(u_id, 'int')}, {to_sql_val(username)}, {to_sql_val(email)}, "
                   f"{to_sql_val(phone)}, {to_sql_val(avatar)}, {to_sql_val(hashed_pass)}, "
                   f"'USER', {to_sql_val(is_active, 'boolean')}, "
                   f"{to_sql_val(created, 'datetime')}, {to_sql_val(created, 'datetime')});")
            sql_statements.append(sql)
    sql_statements.append("")

    # 3. Parse user preferences
    pref_csv_path = os.path.join(CSV_DIR, "user-preferences.csv")
    sql_statements.append("-- Populate auth.user_preferences")
    with open(pref_csv_path, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            p_id = row['preferences_id']
            u_id = row['user_id']
            lang = row['language']
            curr = row['currency']
            theme = row['theme']
            created = row['created_date']
            updated = row['last_updated']
            
            sql = (f"INSERT INTO auth.user_preferences (id, user_id, language, currency, theme, "
                   f"timezone, created_at, updated_at) VALUES ("
                   f"{to_sql_val(p_id, 'int')}, {to_sql_val(u_id, 'int')}, {to_sql_val(lang)}, "
                   f"{to_sql_val(curr)}, {to_sql_val(theme)}, 'Asia/Ho_Chi_Minh', "
                   f"{to_sql_val(created, 'datetime')}, {to_sql_val(updated, 'datetime')});")
            sql_statements.append(sql)
    sql_statements.append("")

    # 4. Parse refresh tokens
    tokens_csv_path = os.path.join(CSV_DIR, "refresh-tokens.csv")
    sql_statements.append("-- Populate auth.refresh_tokens")
    with open(tokens_csv_path, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            t_id = row['token_id']
            u_id = row['user_id']
            token_hash = row['token_hash']
            issued = row['issued_at']
            expires = row['expires_at']
            revoked = row['is_revoked']
            
            sql = (f"INSERT INTO auth.refresh_tokens (id, user_id, token, device_info, expires_at, "
                   f"created_at, revoked) VALUES ("
                   f"{to_sql_val(t_id, 'int')}, {to_sql_val(u_id, 'int')}, {to_sql_val(token_hash)}, "
                   f"'Unknown', {to_sql_val(expires, 'datetime')}, {to_sql_val(issued, 'datetime')}, "
                   f"{to_sql_val(revoked, 'boolean')});")
            sql_statements.append(sql)
    sql_statements.append("")

    # 5. Parse categories
    categories_csv_path = os.path.join(CSV_DIR, "categories.csv")
    sql_statements.append("-- Populate wallet.categories")
    categories = []
    with open(categories_csv_path, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            categories.append(row)

    # Sort categories: insert parent_id IS NULL first, then children, to avoid FK constraint issues
    # depth = 0 first, then depth = 1, etc.
    categories.sort(key=lambda x: int(x['depth']))

    # Determine type (INCOME vs EXPENSE) for each category
    category_types = {}
    category_names = {}
    for cat in categories:
        c_id = cat['category_id']
        parent_id = cat['parent_id'].strip()
        c_name = cat['category_name'].strip()
        category_names[c_id] = c_name
        
        if parent_id == '':
            # Root category
            if c_id == '8' or 'thu nhập' in c_name.lower() or 'lương' in c_name.lower():
                category_types[c_id] = 'INCOME'
            else:
                category_types[c_id] = 'EXPENSE'
        else:
            # Subcategory inherits from parent
            category_types[c_id] = category_types.get(parent_id, 'EXPENSE')

    for cat in categories:
        c_id = cat['category_id']
        u_id = cat['user_id']
        c_name = cat['category_name']
        parent_id = cat['parent_id']
        depth = int(cat['depth']) + 1 # Convert 0-indexed to 1-indexed for check constraint (1-3)
        is_active = cat['is_active']
        icon = cat['icon']
        color = cat['color']
        created = cat['created_date']
        c_type = category_types[c_id]

        sql = (f"INSERT INTO wallet.categories (id, name, parent_id, user_id, icon_path, color, "
               f"type, depth, sort_order, created_at) VALUES ("
               f"{to_sql_val(c_id, 'int')}, {to_sql_val(c_name)}, {to_sql_val(parent_id, 'int')}, "
               f"{to_sql_val(u_id, 'int')}, {to_sql_val(icon)}, {to_sql_val(color)}, "
               f"'{c_type}', {depth}, 0, {to_sql_val(created, 'datetime')});")
        sql_statements.append(sql)
    sql_statements.append("")

    # 6. Parse wallets
    wallets_csv_path = os.path.join(CSV_DIR, "wallets.csv")
    sql_statements.append("-- Populate wallet.wallets")
    wallet_currencies = {}
    with open(wallets_csv_path, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            w_id = row['wallet_id']
            u_id = row['user_id']
            w_name = row['wallet_name']
            balance = row['balance']
            currency = row['currency']
            wallet_currencies[w_id] = currency
            is_active = row['is_active']
            desc = row['description']
            created = row['created_date']
            updated = row['updated_date']
            
            # Map type and icon based on name
            name_lower = w_name.lower()
            if 'vcb' in name_lower or 'bank' in name_lower or 'tài khoản' in name_lower or 'phụ cấp' in name_lower:
                w_type = 'BANK'
                icon = 'icon_bank'
            elif 'thẻ' in name_lower or 'card' in name_lower:
                w_type = 'CARD'
                icon = 'icon_card'
            elif 'chung' in name_lower or 'gia đình' in name_lower:
                w_type = 'SHARED'
                icon = 'icon_shared'
            else:
                w_type = 'CASH'
                icon = 'icon_cash'
                
            # Currency symbol mapping
            curr_upper = currency.upper()
            if curr_upper == 'VND':
                symbol = '₫'
            elif curr_upper == 'USD':
                symbol = '$'
            elif curr_upper == 'EUR':
                symbol = '€'
            elif curr_upper == 'GBP':
                symbol = '£'
            else:
                symbol = '₫'

            sql = (f"INSERT INTO wallet.wallets (id, user_id, family_id, name, type, currency, "
                   f"currency_symbol, balance, icon, is_active, is_deleted, created_at, updated_at) VALUES ("
                   f"{to_sql_val(w_id, 'int')}, {to_sql_val(u_id, 'int')}, NULL, {to_sql_val(w_name)}, "
                   f"'{w_type}', {to_sql_val(currency)}, '{symbol}', {to_sql_val(balance, 'float')}, "
                   f"'{icon}', {to_sql_val(is_active, 'boolean')}, false, "
                   f"{to_sql_val(created, 'datetime')}, {to_sql_val(updated, 'datetime')});")
            sql_statements.append(sql)
    sql_statements.append("")

    # 7. Parse transactions
    tx_csv_path = os.path.join(CSV_DIR, "transactions.csv")
    sql_statements.append("-- Populate wallet.transactions and transaction.transactions")
    with open(tx_csv_path, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            t_id = row['transaction_id']
            w_id = row['wallet_id']
            u_id = row['user_id']
            c_id = row['category_id']
            amount = row['amount']
            t_type = row['type']
            status = row['status']
            desc = row['description']
            is_rec = row['is_recurring']
            created = row['created_date']
            t_date = row['transaction_date']
            
            # Insert into wallet.transactions
            sql_wallet = (f"INSERT INTO wallet.transactions (id, user_id, wallet_id, category_id, "
                          f"amount, type, note, transaction_date, location_json, is_recurring, "
                          f"recurring_id, created_at, updated_at) VALUES ("
                          f"{to_sql_val(t_id, 'int')}, {to_sql_val(u_id, 'int')}, {to_sql_val(w_id, 'int')}, "
                          f"{to_sql_val(c_id, 'int')}, {to_sql_val(amount, 'float')}, {to_sql_val(t_type)}, "
                          f"{to_sql_val(desc)}, {to_sql_val(t_date, 'datetime')}, NULL, "
                          f"{to_sql_val(is_rec, 'boolean')}, NULL, "
                          f"{to_sql_val(created, 'datetime')}, {to_sql_val(created, 'datetime')});")
            sql_statements.append(sql_wallet)
            
            # Insert into transaction.transactions
            curr = wallet_currencies.get(w_id, 'VND')
            sql_txn = (f"INSERT INTO transaction.transactions (id, user_id, wallet_id, category_id, "
                       f"amount, currency, type, transaction_date, description, note, location, "
                       f"is_recurring, recurring_transaction_id, status, source, created_at, updated_at) VALUES ("
                       f"{to_sql_val(t_id, 'int')}, {to_sql_val(u_id, 'int')}, {to_sql_val(w_id, 'int')}, "
                       f"{to_sql_val(c_id, 'int')}, {to_sql_val(amount, 'float')}, {to_sql_val(curr)}, "
                       f"{to_sql_val(t_type)}, {to_sql_val(t_date, 'datetime')}, {to_sql_val(desc)}, "
                       f"{to_sql_val(desc)}, NULL, {to_sql_val(is_rec, 'boolean')}, NULL, "
                       f"{to_sql_val(status)}, 'MANUAL', {to_sql_val(created, 'datetime')}, "
                       f"{to_sql_val(created, 'datetime')});")
            sql_statements.append(sql_txn)
    sql_statements.append("")

    # 8. Parse monthly summaries
    ms_csv_path = os.path.join(CSV_DIR, "monthly-summaries.csv")
    sql_statements.append("-- Populate reporting.monthly_summaries")
    with open(ms_csv_path, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            s_id = row['summary_id']
            u_id = row['user_id']
            month = row['month']
            t_income = row['total_income']
            t_expense = row['total_expense']
            savings = row['savings']
            created = row['created_date']
            
            # Simple average daily expense calculation
            avg_daily = float(t_expense) / 30.0 if t_expense else 0.0
            
            sql = (f"INSERT INTO reporting.monthly_summaries (id, user_id, year_month, total_income, "
                   f"total_expense, net_income, transaction_count, avg_daily_expense, top_expense_category, "
                   f"created_at, updated_at) VALUES ("
                   f"{to_sql_val(s_id, 'int')}, {to_sql_val(u_id, 'int')}, {to_sql_val(month)}, "
                   f"{to_sql_val(t_income, 'float')}, {to_sql_val(t_expense, 'float')}, "
                   f"{to_sql_val(savings, 'float')}, 1, {avg_daily}, NULL, "
                   f"{to_sql_val(created, 'datetime')}, {to_sql_val(created, 'datetime')});")
            sql_statements.append(sql)
    sql_statements.append("")

    # 9. Parse budgets
    budgets_csv_path = os.path.join(CSV_DIR, "budgets.csv")
    sql_statements.append("-- Populate reporting.budgets")
    with open(budgets_csv_path, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            b_id = row['budget_id']
            u_id = row['user_id']
            c_id = row['category_id']
            limit = row['amount_limit']
            used = row['amount_used']
            status = row['status']
            created = row['created_date']
            updated = row['updated_date']
            
            c_name = category_names.get(c_id, 'Unknown')
            year_month = created[:7]
            is_active = 'true' if status in ('ACTIVE', 'WARNING') else 'false'
            
            sql = (f"INSERT INTO reporting.budgets (id, user_id, category_id, category_name, "
                   f"amount_limit, amount_used, period, year_month, is_active, created_at, updated_at) VALUES ("
                   f"{to_sql_val(b_id, 'int')}, {to_sql_val(u_id, 'int')}, {to_sql_val(c_id, 'int')}, "
                   f"{to_sql_val(c_name)}, {to_sql_val(limit, 'float')}, {to_sql_val(used, 'float')}, "
                   f"'MONTHLY', {to_sql_val(year_month)}, {is_active}, "
                   f"{to_sql_val(created, 'datetime')}, {to_sql_val(updated, 'datetime')});")
            sql_statements.append(sql)
    sql_statements.append("")

    # 10. Parse reports
    reports_csv_path = os.path.join(CSV_DIR, "reports.csv")
    sql_statements.append("-- Populate reporting.reports")
    with open(reports_csv_path, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            r_id = row['report_id']
            u_id = row['user_id']
            created = row['generated_date']
            year_month = created[:7]
            
            file_name = f"report_{r_id}.pdf"
            file_url = f"http://storage.example.com/reports/{file_name}"
            
            sql = (f"INSERT INTO reporting.reports (id, user_id, report_type, period, file_name, "
                   f"file_url, file_size, status, created_at) VALUES ("
                   f"{to_sql_val(r_id, 'int')}, {to_sql_val(u_id, 'int')}, 'MONTHLY', "
                   f"{to_sql_val(year_month)}, '{file_name}', '{file_url}', 1024, "
                   f"'COMPLETED', {to_sql_val(created, 'datetime')});")
            sql_statements.append(sql)
    sql_statements.append("")

    # 11. Sync identity sequences for postgres
    sql_statements.append("-- Sync serial sequences to prevent duplicate key errors in future inserts")
    sql_statements.append("SELECT setval('auth.users_id_seq', COALESCE((SELECT MAX(id)+1 FROM auth.users), 1), false);")
    sql_statements.append("SELECT setval('auth.user_preferences_id_seq', COALESCE((SELECT MAX(id)+1 FROM auth.user_preferences), 1), false);")
    sql_statements.append("SELECT setval('auth.refresh_tokens_id_seq', COALESCE((SELECT MAX(id)+1 FROM auth.refresh_tokens), 1), false);")
    sql_statements.append("SELECT setval('wallet.categories_id_seq', COALESCE((SELECT MAX(id)+1 FROM wallet.categories), 1), false);")
    sql_statements.append("SELECT setval('wallet.wallets_id_seq', COALESCE((SELECT MAX(id)+1 FROM wallet.wallets), 1), false);")
    sql_statements.append("SELECT setval('wallet.transactions_id_seq', COALESCE((SELECT MAX(id)+1 FROM wallet.transactions), 1), false);")
    sql_statements.append("SELECT setval('transaction.transactions_id_seq', COALESCE((SELECT MAX(id)+1 FROM transaction.transactions), 1), false);")
    sql_statements.append("SELECT setval('reporting.monthly_summaries_id_seq', COALESCE((SELECT MAX(id)+1 FROM reporting.monthly_summaries), 1), false);")
    sql_statements.append("SELECT setval('reporting.budgets_id_seq', COALESCE((SELECT MAX(id)+1 FROM reporting.budgets), 1), false);")
    sql_statements.append("SELECT setval('reporting.reports_id_seq', COALESCE((SELECT MAX(id)+1 FROM reporting.reports), 1), false);")
    sql_statements.append("")
    sql_statements.append("SELECT 'DATA IMPORT COMPLETED SUCCESSFULLY' as status;")

    # Write to output file
    with open(OUTPUT_SQL, mode='w', encoding='utf-8') as f:
        f.write("\n".join(sql_statements))
    
    print(f"Generated SQL script at {OUTPUT_SQL}")

    # Copy SQL file to PostgreSQL container
    print("Copying SQL script to the postgres container...")
    try:
        subprocess.run(["docker", "cp", OUTPUT_SQL, "fpm-monolith-postgres:/tmp/import_data.sql"], check=True)
        print("Executing SQL script inside the postgres container...")
        result = subprocess.run([
            "docker", "exec", "fpm-monolith-postgres", 
            "psql", "-U", "dev", "-d", "fpm_db", "-f", "/tmp/import_data.sql"
        ], capture_output=True, text=True, check=True)
        print(result.stdout)
        print("Import completed successfully!")
    except subprocess.CalledProcessError as e:
        print(f"Error executing command: {e}")
        print(f"Stdout: {e.stdout}")
        print(f"Stderr: {e.stderr}")

if __name__ == "__main__":
    main()
