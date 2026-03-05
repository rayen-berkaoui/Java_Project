-- =============================================
-- Migration: Add ALL missing tables and columns
-- that the Java codebase expects but are not
-- present in the current tabaany database.
--
-- Current state: only audit_log, otp_codes, role,
-- role_permissions, utilisateur exist.
-- =============================================

USE tabaany;

-- =============================================
-- 1. utilisateur: add missing columns
--    loyalty_points (used by utilisateur entity)
--    theme_preference (used by utilisateur entity)
-- =============================================

ALTER TABLE `utilisateur`
  ADD COLUMN IF NOT EXISTS `loyalty_points` INT(11) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS `theme_preference` VARCHAR(50) DEFAULT 'SYSTEM';

-- =============================================
-- 2. adresse table (entire table missing)
--    Used by: Adresse.java, AdresseService
-- =============================================

CREATE TABLE IF NOT EXISTS `adresse` (
  `id_adresse` INT(11) NOT NULL AUTO_INCREMENT,
  `rue` VARCHAR(255) NOT NULL,
  `ville` VARCHAR(100) NOT NULL,
  `latitude` DOUBLE NOT NULL,
  `longitude` DOUBLE NOT NULL,
  `altitude` DOUBLE DEFAULT NULL,
  PRIMARY KEY (`id_adresse`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =============================================
-- 3. categorie table (entire table missing)
--    Used by: categorie.java, CategorieService
-- =============================================

CREATE TABLE IF NOT EXISTS `categorie` (
  `id_categorie` INT(11) NOT NULL AUTO_INCREMENT,
  `nom_categorie` VARCHAR(100) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `date_creation` DATE DEFAULT NULL,
  PRIMARY KEY (`id_categorie`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =============================================
-- 4. activite table (entire table missing)
--    Referenced in etablissement_activite
-- =============================================

CREATE TABLE IF NOT EXISTS `activite` (
  `idActivite` INT(11) NOT NULL AUTO_INCREMENT,
  `nomActivite` VARCHAR(255) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `categorie` VARCHAR(100) DEFAULT NULL,
  `duree` INT(11) DEFAULT NULL,
  `niveau` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`idActivite`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =============================================
-- 5. etablissement table (entire table missing)
--    Used by: Etablissement.java, PanierService
-- =============================================

CREATE TABLE IF NOT EXISTS `etablissement` (
  `idEtablissement` INT(11) NOT NULL AUTO_INCREMENT,
  `nom` VARCHAR(255) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `adresse` VARCHAR(255) NOT NULL,
  `ville` VARCHAR(100) NOT NULL,
  `telephone` VARCHAR(20) DEFAULT NULL,
  `email` VARCHAR(150) DEFAULT NULL,
  `horaires` VARCHAR(255) DEFAULT NULL,
  `gammePrix` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`idEtablissement`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =============================================
-- 6. etablissement_activite join table (missing)
-- =============================================

CREATE TABLE IF NOT EXISTS `etablissement_activite` (
  `idEtablissement` INT(11) NOT NULL,
  `idActivite` INT(11) NOT NULL,
  `prix` DECIMAL(10,2) NOT NULL,
  `disponible` TINYINT(1) NOT NULL DEFAULT 1,
  `capacite` INT(11) NOT NULL,
  PRIMARY KEY (`idEtablissement`, `idActivite`),
  CONSTRAINT `fk_ea_etablissement` FOREIGN KEY (`idEtablissement`) REFERENCES `etablissement` (`idEtablissement`) ON UPDATE CASCADE,
  CONSTRAINT `fk_ea_activite` FOREIGN KEY (`idActivite`) REFERENCES `activite` (`idActivite`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =============================================
-- 7. lieu table (entire table missing)
--    Used by: Lieu.java, LieuService (some queries)
-- =============================================

CREATE TABLE IF NOT EXISTS `lieu` (
  `id_lieu` INT(11) NOT NULL AUTO_INCREMENT,
  `nom` VARCHAR(150) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `ville` VARCHAR(100) DEFAULT NULL,
  `prix` DECIMAL(10,2) DEFAULT NULL,
  `image` VARCHAR(255) DEFAULT NULL,
  `statut` TINYINT(1) DEFAULT 1,
  `id_categorie` INT(11) NOT NULL,
  `id_adresse` INT(11) NOT NULL,
  PRIMARY KEY (`id_lieu`),
  KEY `fk_lieu_categorie` (`id_categorie`),
  KEY `fk_lieu_adresse` (`id_adresse`),
  CONSTRAINT `fk_lieu_categorie` FOREIGN KEY (`id_categorie`) REFERENCES `categorie` (`id_categorie`) ON UPDATE CASCADE,
  CONSTRAINT `fk_lieu_adresse` FOREIGN KEY (`id_adresse`) REFERENCES `adresse` (`id_adresse`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =============================================
-- 8. lieu_touristique table (entire table missing)
--    Used by: LieuTouristique.java,
--             LieuTouristiqueServices.java, LieuService.java
-- =============================================

CREATE TABLE IF NOT EXISTS `lieu_touristique` (
  `id_lieu` INT(11) NOT NULL AUTO_INCREMENT,
  `nom` VARCHAR(150) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `ville` VARCHAR(100) DEFAULT NULL,
  `prix` DECIMAL(10,2) DEFAULT NULL,
  `image` VARCHAR(255) DEFAULT NULL,
  `statut` INT(11) DEFAULT 1 COMMENT '1=disponible, 0=indisponible',
  `id_categorie` INT(11) NOT NULL,
  `id_adresse` INT(11) NOT NULL,
  PRIMARY KEY (`id_lieu`),
  KEY `fk_lt_categorie` (`id_categorie`),
  KEY `fk_lt_adresse` (`id_adresse`),
  CONSTRAINT `fk_lt_categorie` FOREIGN KEY (`id_categorie`) REFERENCES `categorie` (`id_categorie`) ON UPDATE CASCADE,
  CONSTRAINT `fk_lt_adresse` FOREIGN KEY (`id_adresse`) REFERENCES `adresse` (`id_adresse`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =============================================
-- 9. panier table (entire table missing)
--    Used by: Panier.java, PanierService.java
--    Includes nb_chambres which was also missing
-- =============================================

CREATE TABLE IF NOT EXISTS `panier` (
  `id_panier` INT(11) NOT NULL AUTO_INCREMENT,
  `id_client` INT(11) NOT NULL,
  `id_etablissement` INT(11) NOT NULL,
  `type_service` ENUM('Voyage','Restaurant','Café') NOT NULL,
  `date_debut` DATETIME NOT NULL,
  `date_fin` DATETIME NOT NULL,
  `nb_personnes` INT(11) NOT NULL,
  `prix_estime` DECIMAL(10,2) NOT NULL,
  `statut_item` ENUM('en_attente','annulé','confirme') NOT NULL DEFAULT 'en_attente',
  `nb_adultes` INT(11) DEFAULT 1,
  `nb_enfants` INT(11) DEFAULT 0,
  `nb_chambres` INT(11) DEFAULT 1,
  PRIMARY KEY (`id_panier`),
  KEY `fk_panier_client` (`id_client`),
  KEY `fk_panier_etablissement` (`id_etablissement`),
  CONSTRAINT `fk_panier_client` FOREIGN KEY (`id_client`) REFERENCES `utilisateur` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_panier_etablissement` FOREIGN KEY (`id_etablissement`) REFERENCES `etablissement` (`idEtablissement`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =============================================
-- 10. reservation table (entire table missing)
--     Used by: Reservation.java, ReservationService.java
--     Includes rating + review_comment
-- =============================================

CREATE TABLE IF NOT EXISTS `reservation` (
  `id_reservation` INT(11) NOT NULL AUTO_INCREMENT,
  `id_panier` INT(11) NOT NULL,
  `date_paiement` DATETIME NOT NULL,
  `montant_total` DECIMAL(10,2) NOT NULL,
  `mode_paiement` VARCHAR(50) NOT NULL,
  `statut_paiement` ENUM('Payé','En cours de paiement','Remboursé') NOT NULL,
  `code_confirmation` VARCHAR(255) NOT NULL,
  `rating` INT(11) DEFAULT NULL,
  `review_comment` TEXT DEFAULT NULL,
  PRIMARY KEY (`id_reservation`),
  UNIQUE KEY `code_confirmation` (`code_confirmation`),
  KEY `fk_reservation_panier` (`id_panier`),
  CONSTRAINT `fk_reservation_panier` FOREIGN KEY (`id_panier`) REFERENCES `panier` (`id_panier`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =============================================
-- 11. Seed data for categories and addresses
--     (needed so FK references work for lieu tables)
-- =============================================

INSERT IGNORE INTO `categorie` (`id_categorie`, `nom_categorie`, `description`, `date_creation`) VALUES
(1, 'Historique', 'Sites et monuments historiques', '2025-01-01'),
(2, 'Nature', 'Plages, parcs et espaces naturels', '2025-01-01'),
(3, 'Aventure', 'Excursions et activites sportives', '2025-01-01'),
(4, 'Culture', 'Musees, galeries et art', '2025-01-01'),
(5, 'Religieux', 'Mosquees, eglises et sites spirituels', '2025-01-01');

INSERT IGNORE INTO `adresse` (`id_adresse`, `rue`, `ville`, `latitude`, `longitude`, `altitude`) VALUES
(1, 'Rue de la Kasbah', 'Tunis', 36.8065, 10.1815, 10),
(2, 'Avenue Habib Bourguiba', 'Sidi Bou Said', 36.8692, 10.3497, 5),
(3, 'Route de Douz', 'Douz', 33.45, 8.1167, 85),
(4, 'Centre Ville', 'El Jem', 35.2961, 10.7139, 95),
(5, 'Avenue de la Republique', 'Tunis', 36.8, 10.17, 12),
(6, 'Route Touristique', 'Djerba', 33.8076, 10.8451, 3),
(7, 'Rue des Oliviers', 'Hammamet', 36.4, 10.6167, 8),
(8, 'Borj Cedria', 'Ben Arous', 36.72, 10.23, 15),
(9, 'Avenue de Carthage', 'Carthage', 36.8528, 10.3233, 20),
(10, 'Rue de la Liberte', 'Sousse', 35.8256, 10.6369, 10),
(11, 'Rue du Port', 'La Marsa', 36.8783, 10.325, 5),
(12, 'Centre Ville', 'Korbous', 36.8167, 10.5667, 30);

INSERT IGNORE INTO `etablissement` (`idEtablissement`, `nom`, `description`, `adresse`, `ville`, `telephone`, `email`, `horaires`, `gammePrix`) VALUES
(1, 'Le Baroque', 'Cuisine italienne raffinee avec vue sur mer.', 'Avenue de la Plage', 'La Marsa', '71 123 456', 'contact@lebaroque.tn', '12:00-23:00', 'Moyen'),
(2, 'Cafe des Delices', 'Cafe emblematique avec vue panoramique.', 'Rue Habib Thameur', 'Sidi Bou Said', '71 234 567', 'info@cafedelices.tn', '08:00-22:00', 'Bas'),
(3, 'Aqua Palace', 'Parc aquatique familial.', 'Zone Touristique', 'Hammamet', '72 345 678', 'contact@aquapalace.tn', '09:00-18:00', 'Eleve'),
(4, 'Thermes de Jebel', 'Sources thermales naturelles.', 'Route Thermale', 'Korbous', '72 456 789', 'spa@thermesjebel.tn', '08:00-20:00', 'Moyen'),
(5, 'Club Sportif Carthage', 'Centre sportif multifonction.', 'Avenue Hannibal', 'Carthage', '71 567 890', 'info@cscarthage.tn', '06:00-22:00', 'Moyen'),
(6, 'Dar El Jeld', 'Restaurant gastronomique tunisien.', 'Rue Dar El Jeld', 'Tunis', '71 678 901', 'reservation@dareljeld.tn', '12:00-23:00', 'Eleve');

INSERT IGNORE INTO `role` (`id`, `nom`, `description`) VALUES
(1, 'TOURISTE', 'Utilisateur touriste'),
(2, 'PARTENAIRE', 'Utilisateur partenaire'),
(3, 'ADMIN', 'Administrateur systeme');

-- =============================================
-- Done! All missing tables, columns, and seed data added.
-- =============================================
