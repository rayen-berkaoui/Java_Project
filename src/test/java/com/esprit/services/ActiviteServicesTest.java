package com.esprit.services;

import com.esprit.entities.Activite;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActiviteServicesTest {

    private Connection cnx;
    private ActiviteServices service;

    @BeforeEach
    void setUp() throws Exception {
        cnx = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        service = new ActiviteServices(cnx);

        try (Statement st = cnx.createStatement()) {
            st.execute("""
                DROP TABLE IF EXISTS activite;
                """);

            st.execute("""
                CREATE TABLE activite (
                    idActivite INT AUTO_INCREMENT PRIMARY KEY,
                    nomActivite VARCHAR(255) NOT NULL,
                    description VARCHAR(500),
                    categorie VARCHAR(100),
                    duree INT,
                    niveau VARCHAR(50),
                    image_url VARCHAR(500)
                );
                """);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        cnx.close();
    }

    @Test
    void testAjouterEtAfficher() throws Exception {
        Activite a = new Activite("Yoga", "desc", "Sport", 60, "Débutant", "C:\\img.jpg");
        service.ajouter(a);

        List<Activite> list = service.afficher();
        assertEquals(1, list.size());
        assertEquals("Yoga", list.get(0).getNomActivite());
        assertEquals("C:\\img.jpg", list.get(0).getImageUrl());
    }

    @Test
    void testModifier() throws Exception {
        service.ajouter(new Activite("A", "d", "Sport", 30, "Débutant", "x"));

        Activite saved = service.afficher().get(0);
        saved.setNomActivite("B");
        saved.setDuree(90);
        saved.setImageUrl("new.png");

        service.modifier(saved);

        Activite updated = service.afficher().get(0);
        assertEquals("B", updated.getNomActivite());
        assertEquals(90, updated.getDuree());
        assertEquals("new.png", updated.getImageUrl());
    }

    @Test
    void testSupprimer() throws Exception {
        service.ajouter(new Activite("A", "d", "Sport", 30, "Débutant", "x"));

        Activite saved = service.afficher().get(0);
        service.supprimer(saved.getIdActivite());

        assertTrue(service.afficher().isEmpty());
    }
}
