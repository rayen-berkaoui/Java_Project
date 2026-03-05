-- ========================================================
-- Migration: Integration g_resto into tabaany project
-- ========================================================

-- 1. Add missing columns to etablissement
ALTER TABLE `etablissement`
  ADD COLUMN `type` VARCHAR(50) DEFAULT 'autre' AFTER `gammePrix`,
  ADD COLUMN `latitude` DOUBLE DEFAULT NULL AFTER `type`,
  ADD COLUMN `longitude` DOUBLE DEFAULT NULL AFTER `latitude`;

-- Update existing etablissements with type based on name
UPDATE `etablissement` SET `type` = 'restaurant' WHERE LOWER(`nom`) LIKE '%baroque%' OR LOWER(`nom`) LIKE '%dar el jeld%';
UPDATE `etablissement` SET `type` = 'cafe' WHERE LOWER(`nom`) LIKE '%cafe%' OR LOWER(`nom`) LIKE '%café%';
UPDATE `etablissement` SET `type` = 'hotel' WHERE LOWER(`nom`) LIKE '%hotel%';
UPDATE `etablissement` SET `type` = 'bar' WHERE LOWER(`nom`) LIKE '%bar%';
UPDATE `etablissement` SET `type` = 'autre' WHERE `type` IS NULL OR `type` = '';

-- 2. Add language to utilisateur
ALTER TABLE `utilisateur`
  ADD COLUMN `language` VARCHAR(10) DEFAULT 'fr' AFTER `theme_preference`;

-- 3. Expand activite table with all g_resto fields (snake_case to match services)
ALTER TABLE `activite`
  ADD COLUMN `prix` DECIMAL(10,2) DEFAULT NULL AFTER `niveau`,
  ADD COLUMN `devise` VARCHAR(10) DEFAULT 'TND' AFTER `prix`,
  ADD COLUMN `date_debut` TIMESTAMP NULL DEFAULT NULL AFTER `devise`,
  ADD COLUMN `date_fin` TIMESTAMP NULL DEFAULT NULL AFTER `date_debut`,
  ADD COLUMN `nb_places` INT(11) DEFAULT 0 AFTER `date_fin`,
  ADD COLUMN `places_dispo` INT(11) DEFAULT 0 AFTER `nb_places`,
  ADD COLUMN `adresse_depart` VARCHAR(255) DEFAULT NULL AFTER `places_dispo`,
  ADD COLUMN `age_min` INT(11) DEFAULT 0 AFTER `adresse_depart`,
  ADD COLUMN `equipement_inclus` TEXT DEFAULT NULL AFTER `age_min`,
  ADD COLUMN `conditions_annulation` TEXT DEFAULT NULL AFTER `equipement_inclus`,
  ADD COLUMN `statut` VARCHAR(50) DEFAULT 'disponible' AFTER `conditions_annulation`,
  ADD COLUMN `idEtablissement` INT(11) DEFAULT NULL AFTER `statut`;

-- Add FK for activite -> etablissement  
ALTER TABLE `activite`
  ADD CONSTRAINT `fk_activite_etablissement` FOREIGN KEY (`idEtablissement`) REFERENCES `etablissement` (`idEtablissement`) ON DELETE SET NULL ON UPDATE CASCADE;

-- 4. Create etablissement_image table
CREATE TABLE IF NOT EXISTS `etablissement_image` (
  `idImage` INT(11) NOT NULL AUTO_INCREMENT,
  `idEtablissement` INT(11) NOT NULL,
  `imagePath` VARCHAR(500) NOT NULL,
  `ordreAffichage` INT(11) DEFAULT 0,
  PRIMARY KEY (`idImage`),
  KEY `fk_etab_image` (`idEtablissement`),
  CONSTRAINT `fk_etab_image` FOREIGN KEY (`idEtablissement`) REFERENCES `etablissement` (`idEtablissement`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 5. Create activite_image table
CREATE TABLE IF NOT EXISTS `activite_image` (
  `idImage` INT(11) NOT NULL AUTO_INCREMENT,
  `idActivite` INT(11) NOT NULL,
  `imagePath` VARCHAR(500) NOT NULL,
  `ordreAffichage` INT(11) DEFAULT 0,
  PRIMARY KEY (`idImage`),
  KEY `fk_act_image` (`idActivite`),
  CONSTRAINT `fk_act_image` FOREIGN KEY (`idActivite`) REFERENCES `activite` (`idActivite`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
