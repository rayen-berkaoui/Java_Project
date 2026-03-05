-- =============================================
-- FULL SCHEMA FOR tabaany DATABASE
-- Order: tables with no FK first, then dependent tables
-- =============================================

USE tabaany;

-- =============================================
-- 1. INDEPENDENT TABLES (no foreign keys)
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

CREATE TABLE IF NOT EXISTS `categorie` (
  `id_categorie` INT(11) NOT NULL AUTO_INCREMENT,
  `nom_categorie` VARCHAR(100) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `date_creation` DATE DEFAULT NULL,
  PRIMARY KEY (`id_categorie`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `role` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `nom` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nom` (`nom`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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

CREATE TABLE IF NOT EXISTS `activite` (
  `idActivite` INT(11) NOT NULL AUTO_INCREMENT,
  `nomActivite` VARCHAR(255) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `categorie` VARCHAR(100) DEFAULT NULL,
  `duree` INT(11) DEFAULT NULL,
  `niveau` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`idActivite`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `otp_codes` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(255) NOT NULL,
  `code` VARCHAR(6) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT current_timestamp(),
  `used` TINYINT(1) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `admin_id` INT(11) NOT NULL,
  `admin_name` VARCHAR(255) DEFAULT NULL,
  `action` VARCHAR(100) DEFAULT NULL,
  `target_type` VARCHAR(50) DEFAULT NULL,
  `target_id` INT(11) DEFAULT 0,
  `target_name` VARCHAR(255) DEFAULT NULL,
  `old_value` TEXT DEFAULT NULL,
  `new_value` TEXT DEFAULT NULL,
  `ip_address` VARCHAR(45) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_audit_action` (`action`),
  KEY `idx_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =============================================
-- 2. TABLES WITH FOREIGN KEYS (level 1)
-- =============================================

CREATE TABLE IF NOT EXISTS `utilisateur` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `nom` VARCHAR(100) NOT NULL,
  `prenom` VARCHAR(100) NOT NULL,
  `email` VARCHAR(150) NOT NULL,
  `mot_de_passe` VARCHAR(255) NOT NULL,
  `statut` ENUM('ACTIF','BLOQUE') DEFAULT 'ACTIF',
  `date_creation` TIMESTAMP NOT NULL DEFAULT current_timestamp(),
  `num_tel` INT(11) NOT NULL,
  `nfc_id` VARCHAR(100) DEFAULT NULL,
  `role_id` INT(11) NOT NULL,
  `profile_picture` LONGTEXT DEFAULT NULL,
  `face_encoding` LONGTEXT DEFAULT NULL,
  `face_confidence` DOUBLE DEFAULT 0,
  `face_samples_count` INT(11) DEFAULT 0,
  `last_face_login` TIMESTAMP NULL DEFAULT NULL,
  `totp_secret` VARCHAR(255) DEFAULT NULL,
  `totp_enabled` TINYINT(1) DEFAULT 0,
  `loyalty_points` INT(11) DEFAULT 0,
  `theme_preference` VARCHAR(50) DEFAULT 'SYSTEM',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `fk_role` (`role_id`),
  KEY `idx_email` (`email`),
  KEY `idx_nfc` (`nfc_id`),
  CONSTRAINT `fk_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `role_permissions` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `role_id` INT(11) NOT NULL,
  `permission_name` VARCHAR(100) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_role_perm` (`role_id`, `permission_name`),
  CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
-- 3. TABLES WITH FOREIGN KEYS (level 2)
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
  CONSTRAINT `fk_panier_client` FOREIGN KEY (`id_client`) REFERENCES `utilisateur` (`id`) ON UPDATE CASCADE
  -- Note: no FK on id_etablissement — it stores either etablissement ID or lieu_touristique ID (type_service distinguishes)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =============================================
-- 4. TABLES WITH FOREIGN KEYS (level 3)
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
-- 5. SEED DATA - Default roles
-- =============================================

INSERT IGNORE INTO `role` (`id`, `nom`, `description`) VALUES
(1, 'ADMIN', 'Administrateur avec accès complet'),
(2, 'TOURISTE', 'Utilisateur touriste standard'),
(3, 'PARTENAIRE', 'Partenaire établissement');

-- Default admin user (password: admin123 - BCrypt hash)
INSERT IGNORE INTO `utilisateur` (`id`, `nom`, `prenom`, `email`, `mot_de_passe`, `statut`, `num_tel`, `role_id`) VALUES
(1, 'Admin', 'System', 'admin@tabaany.tn', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIF', 12345678, 1);

-- Default role permissions for ADMIN
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_name`, `enabled`) VALUES
(1, 'MANAGE_USERS', 1),
(1, 'MANAGE_ROLES', 1),
(1, 'VIEW_AUDIT_LOG', 1),
(1, 'MANAGE_ETABLISSEMENTS', 1),
(1, 'MANAGE_RESERVATIONS', 1),
(1, 'MANAGE_LIEUX', 1);

-- =============================================
-- 6. MIGRATE DATA FROM tabbani (if exists)
-- =============================================

-- Copy adresse data from tabbani
INSERT IGNORE INTO tabaany.adresse
SELECT * FROM tabbani.adresse;

-- Copy categorie data from tabbani
INSERT IGNORE INTO tabaany.categorie
SELECT * FROM tabbani.categorie;

-- Copy lieu_touristique data from tabbani
INSERT IGNORE INTO tabaany.lieu_touristique
SELECT * FROM tabbani.lieu_touristique;
