import java.util.*;
import java.sql.*;
import java.io.*;

class OnlineBankingSystem{
    public static void main(String[] args) throws Exception{
        Scanner sc = new Scanner(System.in);
        String url = "jdbc:mysql://localhost:3306/onlinebankingsystem";
        String user = "root";
        String pass = "";
        String driver = "com.mysql.cj.jdbc.Driver";

        Connection con = DriverManager.getConnection(url, user, pass);

        if(con != null){
            System.out.println("Connection successful");
        }

        BankAccount ba = new BankAccount();

        String accNo = ba.userLogin(con);

        ba.bankAccountDetails(con);

            while(true){

                System.out.println("enter your choice");
                System.out.println("(1) deposit money");
                System.out.println("(2) withdraw money");
                System.out.println("(3) print passbook");
                System.out.println("(4) transfer money to another account");
                System.out.println("(5) get transaction details");
                System.out.println("(6) exit");
                int choice = sc.nextInt();
                sc.nextLine();

                switch(choice){
                    case 1:
                        ba.deposit(con, accNo);
                        break;

                    case 2:
                        ba.withdraw(con, accNo);
                        break;

                    case 3:
                        ba.writeFinal(accNo);
                        ba.print(accNo);
                        break;

                    case 4:
                        System.out.println("Enter bank account number in which you want to transfer money");
                        String toAccNum = sc.nextLine();
                        if(ba.accCheck(con, toAccNum)){
                            ba.transfer(con, accNo, toAccNum);
                        }else{
                            System.out.println("Enter correct account number, please try again");
                        }
                        break;

                    case 5:
                        ba.transactionDetails(con, accNo);
                        break;

                    case 6:
                        System.out.println("Thank You for visiting, see you soon");
                        System.exit(choice);
                        break;

                    default:
                        System.out.println("enter valid choice");
                        break;
                }
            }
        }
    }


class BankAccount {
    Scanner sc = new Scanner(System.in);
    HashMap<String, Double> accounts = new HashMap<>();
    String userLogin(Connection con) throws Exception{

        while(true){
            System.out.println("PRESS 1 : To register your email address");
            System.out.println("PRESS 2 : To login your email address");

            int choice = sc.nextInt();

            if(choice == 1){
                CallableStatement cst = con.prepareCall("{call getUser()}");

                System.out.println("Enter an unique user id");
                int userId = sc.nextInt();
                sc.nextLine();
                boolean check = false;

                ResultSet rs = cst.executeQuery();

                while(rs.next()){
                    if(rs.getInt("user_id") == userId){
                        check = true;
                    }
                }

                if(check){
                    System.out.println("User id already exists");
                }
                else{
                    System.out.println("Enter your user name");
                    String userName = sc.nextLine();
                    System.out.println("Enter your email address");
                    String email = sc.nextLine();
                    System.out.println("Set your password");
                    String pass = sc.nextLine();

                    String regUser = "insert into user values(?,?,?,?)";

                    PreparedStatement pst = con.prepareStatement(regUser);

                    pst.setInt(1, userId);
                    pst.setString(2, userName);
                    pst.setString(3, pass);
                    pst.setString(4, email);
                    pst.execute();

                    openNewAccount(con, email, pass);

                    String acc_no_access = "select account_no from accountDetails where email = ? and password = ?";

                    PreparedStatement pst2 = con.prepareStatement(acc_no_access);
                    pst2.setString(1, email);
                    pst2.setString(2, pass);

                    ResultSet rs2 = pst2.executeQuery();

                    String accNo = "";

                    while(rs2.next()){
                        accNo = rs2.getString("account_no");
                    }
                    System.out.println("You have successfully registered");
                    return accNo;
                }
            }
            else if(choice == 2){
                System.out.println("Enter your user id for login");
                int userId = sc.nextInt();
                sc.nextLine();
                System.out.println("Enter your password");
                String pass = sc.nextLine();
                boolean loginCheck = false;

                CallableStatement cst = con.prepareCall("{call getUser()}");

                ResultSet rs = cst.executeQuery();

                while(rs.next()){
                    if(rs.getInt("user_id") == userId && rs.getString("password").equalsIgnoreCase(pass)){
                        loginCheck = true;
                        break;
                    }
                }
                if(!loginCheck){
                    System.out.println("You have entered either wrong user id or password");
                }else{
                    Statement st = con.createStatement();
                    String email = "";
                    String email_access = "select email from user where user_id = " + userId;
                    ResultSet rs3 = st.executeQuery(email_access);
                    while(rs3.next()){
                        email = rs3.getString("email");
                    }

                    String acc_no_access = "select account_no from accountDetails where email = ? and password = ?";

                    PreparedStatement pst = con.prepareStatement(acc_no_access);
                    pst.setString(1, email);
                    pst.setString(2, pass);

                    ResultSet rs2 = pst.executeQuery();

                    String accNo = "";

                    while(rs2.next()){
                        accNo = rs2.getString("account_no");
                    }
                    System.out.println("You have successfully logged in");
                    return accNo;
                }
            }else{
                System.out.println("Enter valid choice");
            }
        }
    }

    boolean accCheck(Connection con, String accNo) throws Exception{
        boolean check = false;
        if(accNo.length() == 10){
            char[] ch = accNo.toCharArray();

            for(int i = 0; i < ch.length; i++){
                if(Character.isLetter(ch[i])){
                    check = false;
                }else{
                    check = true;
                }
            }
        }
        if(check){

            CallableStatement cst = con.prepareCall("{call getAccountDetails}");

            ResultSet rs1 = cst.executeQuery();

            while (rs1.next()){
                if(rs1.getString("Account_no").equalsIgnoreCase(accNo)){
                    return true;
                }
            }
        }
        return false;
    }

    void bankAccountDetails(Connection con) throws Exception{

        CallableStatement cst = con.prepareCall("{call getAccountDetails()}");

        ResultSet accountDetails = cst.executeQuery();

        while(accountDetails.next()){
            String accNo = accountDetails.getString("Account_no");
            double bal = accountDetails.getDouble("Balance");
            accounts.put(accNo, bal);
        }

        for (Map.Entry<String, Double> element : accounts.entrySet())
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(element.getKey() + ".txt"));
            writer.write("Account number: " + element.getKey());
            writer.newLine();
            writer.write("Opening Balance: " + element.getValue());
            writer.newLine();
            writer.write("--------Transactions--------");
            writer.newLine();
            writer.close();
        }
    }

    void openNewAccount(Connection con, String email, String pass) throws Exception{
        CallableStatement cst = con.prepareCall("{call openNewAccount(?,?,?,?)}");
        System.out.println("you have been allot an account number from the bank");
        long acNo1 = (long)(Math.random() * 1000000000);
        int acNo2 = (int)(Math.random() * 10);
        String accNo = "" + acNo1 + "" + acNo2;
        System.out.println("your account no is " + accNo);
        cst.setString(1, accNo);
        System.out.println("deposit some amount to open your account, 1 Rs. is also enough to open an account");
        double bal = sc.nextDouble();
        sc.nextLine();
        cst.setDouble(2, bal);
        cst.setString(3, email);
        cst.setString(4, pass);
        cst.execute();
        accounts.put(accNo,bal);

        CallableStatement transaction = con.prepareCall("{call setTransactionDetails(?,?,?,?)}");
        long transaction_id = (long)(Math.random() * 100000000);
        transaction.setLong(1, transaction_id);
        transaction.setString(2, accNo);
        transaction.setString(3, "Deposit");
        transaction.setDouble(4, bal);

        BufferedWriter writer = new BufferedWriter(new FileWriter(accNo + ".txt"));
        writer.write("Account number: " + accNo);
        writer.newLine();
        writer.write("Opening Balance: " + bal);
        writer.newLine();
        writer.write("--------Transactions--------");
        writer.newLine();
        writer.close();

    }
    void deposit(Connection con, String Account_no) throws Exception{

        Statement st = con.createStatement();
        String bal = "select balance from accountDetails where account_no = " + Account_no;
        double balance = 0;
        ResultSet rs2 = st.executeQuery(bal);
        while(rs2.next()){
            balance = rs2.getDouble("balance");
        }
        System.out.print("Enter amount: ");
        double amount = sc.nextDouble();
        balance += amount;
        String updateBalance = "update accountDetails set balance = " + balance + " where account_no = " + Account_no;
        st.execute(updateBalance);
        accounts.put(Account_no, accounts.get(Account_no) + amount);
        writeToPassBook(Account_no, amount, accounts.get(Account_no), "Deposited");
        System.out.println("Successfully deposited");

        CallableStatement transaction = con.prepareCall("{call setTransactionDetails(?,?,?,?)}");
        long transaction_id = (long)(Math.random() * 100000000);
        transaction.setLong(1, transaction_id);
        transaction.setString(2, Account_no);
        transaction.setString(3, "Deposit");
        transaction.setDouble(4, amount);
        transaction.execute();
    }


    void withdraw(Connection con, String Account_no) throws Exception{

        Statement st = con.createStatement();
        System.out.print("Enter amount: ");
        double amount = sc.nextDouble();
        String bal = "select balance from accountDetails where account_no = " + Account_no;
        double balance = 0;
        ResultSet rs2 = st.executeQuery(bal);
        while(rs2.next()){
            balance = rs2.getDouble("balance");
        }
        if(balance >= amount){
            balance -= amount;
            String updateBalance = "update accountDetails set balance = " + balance + " where account_no = " + Account_no;
            st.execute(updateBalance);
            accounts.put(Account_no, accounts.get(Account_no) - amount);
            writeToPassBook(Account_no, amount, accounts.get(Account_no), "Withdrawn");
            System.out.println("Successfully withdrawn");

            CallableStatement transaction = con.prepareCall("{call setTransactionDetails(?,?,?,?)}");
            long transaction_id = (long)(Math.random() * 100000000);
            transaction.setLong(1, transaction_id);
            transaction.setString(2, Account_no);
            transaction.setString(3, "Withdraw");
            transaction.setDouble(4, amount);
            transaction.execute();
        }else{
            System.out.println("Insufficient balance, balance is " + balance);
        }
    }

    void transfer(Connection con, String fromAccNum, String toAccNum) throws Exception{
        Statement st = con.createStatement();
        String bal1 = "select balance from accountDetails where account_no = " + fromAccNum;
        String bal2 = "select balance from accountDetails where account_no = " + toAccNum;
        double balance1 = 0;
        double balance2 = 0;
        ResultSet rs1 = st.executeQuery(bal1);
        while(rs1.next()){
            balance1 = rs1.getDouble("balance");
        }
        ResultSet rs2 = st.executeQuery(bal2);
        while(rs2.next()){
            balance2 = rs2.getDouble("balance");
        }
        System.out.println("Enter amount of money you want to transfer");
        double amount = sc.nextDouble();

        if(balance1 >= amount){
            balance1 -= amount;
            String updateBalance1 = "update accountDetails set balance = " + balance1 + " where account_no = " + fromAccNum;
            st.execute(updateBalance1);
            accounts.put(fromAccNum, accounts.get(fromAccNum) - amount);
            writeToPassBook(fromAccNum, amount, accounts.get(fromAccNum), "Debited");
            System.out.println("Successfully transferred to " + toAccNum);

            CallableStatement transaction1 = con.prepareCall("{call setTransactionDetails(?,?,?,?)}");
            long transaction_id1 = (long)(Math.random() * 100000000);
            transaction1.setLong(1, transaction_id1);
            transaction1.setString(2, fromAccNum);
            transaction1.setString(3, "Debited");
            transaction1.setDouble(4, amount);
            transaction1.execute();

            balance2 += amount;
            String updateBalance2 = "update accountDetails set balance = " + balance2 + " where account_no = " + toAccNum;
            st.execute(updateBalance2);
            accounts.put(toAccNum, accounts.get(toAccNum) + amount);
            writeToPassBook(toAccNum, amount, accounts.get(toAccNum), "Credited");
            System.out.println("Successfully credited money from " + fromAccNum);

            CallableStatement transaction2 = con.prepareCall("{call setTransactionDetails(?,?,?,?)}");
            long transaction_id2 = (long)(Math.random() * 100000000);
            transaction2.setLong(1, transaction_id1);
            transaction2.setString(2, toAccNum);
            transaction2.setString(3, "Credited");
            transaction2.setDouble(4, amount);
            transaction2.execute();
        }else{
            System.out.println("Insufficient balance");
        }
    }

    void transactionDetails(Connection con, String accNo) throws Exception{
        Statement st = con.createStatement();
        String transactionDetails = "select * from transactionDetails where account_no = " + accNo;
        ResultSet trans = st.executeQuery(transactionDetails);

        while(trans.next()){
            System.out.println("------------------------------------------------------------------");
            System.out.println("Transaction Id : " + trans.getLong("transaction_id"));
            System.out.println("Account number : " + trans.getString("Account_no"));
            System.out.println("Transaction type : " + trans.getString("transaction_type"));
            System.out.println("Amount : " + trans.getDouble("Amount"));
            System.out.println("Time Stamp : " + trans.getTimestamp("timestamp"));
            System.out.println("------------------------------------------------------------------\n");
        }
    }
    void writeToPassBook(String Account_no, double amount, double balance, String message) throws Exception{
        BufferedWriter writer = new BufferedWriter(new FileWriter(Account_no + ".txt", true));
        writer.write(message + "      " + amount + "     " + balance);
        writer.newLine();
        writer.close();
    }

    void writeFinal(String Account_no) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(Account_no + ".txt", true));
        writer.write("Closing Balance :" + accounts.get(Account_no));
        writer.newLine();
        writer.close();
    }

    void print(String Account_No) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(Account_No + ".txt"));
        String line = reader.readLine();
        while (line != null) {
            System.out.println(line);
            line = reader.readLine();
        }
        reader.close();
    }
}
