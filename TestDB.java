import java.sql.*;

public class TestDB {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/tabaany?useSSL=false";
        Connection con = DriverManager.getConnection(url, "root", "");
        System.out.println("Connected: " + !con.isClosed());
        
        Statement st = con.createStatement();
        
        ResultSet rs1 = st.executeQuery("SELECT COUNT(*) FROM categorie");
        rs1.next(); System.out.println("Categories: " + rs1.getInt(1));
        
        ResultSet rs2 = st.executeQuery("SELECT COUNT(*) FROM adresse");
        rs2.next(); System.out.println("Adresses: " + rs2.getInt(1));
        
        ResultSet rs3 = st.executeQuery("SELECT COUNT(*) FROM lieu_touristique");
        rs3.next(); System.out.println("Lieux: " + rs3.getInt(1));
        
        ResultSet rs4 = st.executeQuery("SELECT id_lieu, nom, ville FROM lieu_touristique");
        while(rs4.next()) {
            System.out.println("  " + rs4.getInt(1) + ": " + rs4.getString(2) + " (" + rs4.getString(3) + ")");
        }
        
        con.close();
        System.out.println("Done.");
    }
}
