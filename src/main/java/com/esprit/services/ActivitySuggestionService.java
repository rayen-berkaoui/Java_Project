package com.esprit.services;

import java.util.*;

/**
 * Service fournissant des listes prédéfinies d'équipements et de conditions
 * d'annulation selon la catégorie d'activité sélectionnée.
 *
 * L'utilisateur choisit parmi ces listes via des ComboBox dans le formulaire.
 */
public class ActivitySuggestionService {

    // ── Équipements par catégorie ────────────────────────────────────────
    private static final Map<String, List<String>> EQUIPEMENTS = new LinkedHashMap<>();

    // ── Conditions d'annulation par catégorie ────────────────────────────
    private static final Map<String, List<String>> CONDITIONS = new LinkedHashMap<>();

    static {
        // ═══════════════════════════════════════════════════════════════
        //  ÉQUIPEMENTS — Sport
        // ═══════════════════════════════════════════════════════════════
        EQUIPEMENTS.put("Sport", List.of(
                "Plongée : Combinaison, masque, tuba, palmes, bouteille d'oxygène, gilet stabilisateur",
                "Randonnée : Chaussures de marche, bâtons, sac à dos, gourde 1.5L, chapeau, crème solaire",
                "Escalade : Baudrier, corde, mousquetons, chaussons d'escalade, casque, magnésie",
                "Surf : Planche de surf, combinaison néoprène, leash, wax",
                "Kayak : Kayak, pagaie, gilet de sauvetage, bidon étanche",
                "Vélo / Cyclisme : Vélo, casque, gourde, kit de réparation, gilet réfléchissant",
                "Ski : Skis, bâtons, chaussures de ski, casque, forfait remontées, combinaison",
                "Yoga / Méditation : Tapis de yoga, tenue confortable, serviette, bouteille d'eau",
                "Parachutisme : Combinaison de saut, casque, lunettes, harnais (fourni par l'instructeur)",
                "Voile / Catamaran : Gilet de sauvetage, gants de voile, chaussures antidérapantes",
                "Équitation : Bombe/casque, bottes, pantalon d'équitation",
                "Sport général : Tenue de sport adaptée, chaussures sportives, bouteille d'eau, serviette"
        ));

        // ═══════════════════════════════════════════════════════════════
        //  ÉQUIPEMENTS — Culture
        // ═══════════════════════════════════════════════════════════════
        EQUIPEMENTS.put("Culture", List.of(
                "Visite musée / Exposition : Audioguide fourni, brochure du parcours, carnet de notes",
                "Atelier créatif : Tablier de protection, matériel de création fourni, tenue adaptée",
                "Visite guidée : Brochure informative, badge visiteur, chaussures confortables",
                "Spectacle / Concert : Billet d'entrée, plan de salle",
                "Culture général : Audioguide fourni, brochure informative, badge visiteur"
        ));

        // ═══════════════════════════════════════════════════════════════
        //  ÉQUIPEMENTS — Loisir
        // ═══════════════════════════════════════════════════════════════
        EQUIPEMENTS.put("Loisir", List.of(
                "Parc d'attractions : Bracelet d'accès, plan du parc, tenue confortable",
                "Bowling / Billard : Chaussures fournies sur place, tenue décontractée",
                "Karting : Casque fourni, combinaison, gants",
                "Escape game : Aucun équipement requis, tenue confortable recommandée",
                "Loisir général : Équipement standard fourni sur place, tenue confortable recommandée"
        ));

        // ═══════════════════════════════════════════════════════════════
        //  ÉQUIPEMENTS — Nature
        // ═══════════════════════════════════════════════════════════════
        EQUIPEMENTS.put("Nature", List.of(
                "Safari : Jumelles, chapeau, vêtements kaki/neutres, crème solaire, appareil photo, gourde",
                "Camping / Bivouac : Tente, sac de couchage, matelas, lampe frontale, réchaud, trousse de secours",
                "Observation d'oiseaux : Jumelles, guide ornithologique, vêtements discrets, carnet de notes",
                "Balade en forêt : Chaussures de marche, chapeau, crème solaire, gourde, sac à dos",
                "Nature général : Chaussures de marche, chapeau, crème solaire, jumelles, sac à dos, gourde"
        ));

        // ═══════════════════════════════════════════════════════════════
        //  ÉQUIPEMENTS — Autre
        // ═══════════════════════════════════════════════════════════════
        EQUIPEMENTS.put("Autre", List.of(
                "Aucun équipement spécifique requis",
                "Équipement communiqué avant l'activité",
                "Tenue confortable recommandée"
        ));

        // ═══════════════════════════════════════════════════════════════
        //  CONDITIONS D'ANNULATION — Sport
        // ═══════════════════════════════════════════════════════════════
        CONDITIONS.put("Sport", List.of(
                "Annulation gratuite 48h avant. 48h-24h : remboursement 50%. < 24h : aucun remboursement",
                "Annulation gratuite 72h avant. Certificat médical : remboursement intégral",
                "Conditions météo défavorables : report automatique sans frais",
                "Aucun remboursement après inscription. Report possible sous réserve",
                "Annulation gratuite 24h avant. < 24h : aucun remboursement"
        ));

        // ═══════════════════════════════════════════════════════════════
        //  CONDITIONS D'ANNULATION — Culture
        // ═══════════════════════════════════════════════════════════════
        CONDITIONS.put("Culture", List.of(
                "Annulation gratuite 24h avant. < 24h : remboursement 50%. Non-présentation : aucun remboursement",
                "Billet non remboursable, échangeable jusqu'à 48h avant",
                "Annulation gratuite 48h avant. < 48h : aucun remboursement",
                "Remboursement intégral en cas d'annulation par l'organisateur"
        ));

        // ═══════════════════════════════════════════════════════════════
        //  CONDITIONS D'ANNULATION — Loisir
        // ═══════════════════════════════════════════════════════════════
        CONDITIONS.put("Loisir", List.of(
                "Annulation gratuite 24h avant. < 24h : aucun remboursement. Report possible",
                "Aucun remboursement. Report possible sous réserve de disponibilité",
                "Annulation gratuite 48h avant. < 48h : remboursement 50%",
                "Remboursement intégral en cas d'annulation par l'organisateur"
        ));

        // ═══════════════════════════════════════════════════════════════
        //  CONDITIONS D'ANNULATION — Nature
        // ═══════════════════════════════════════════════════════════════
        CONDITIONS.put("Nature", List.of(
                "Annulation gratuite 48h avant. Météo défavorable : report ou remboursement intégral",
                "Annulation gratuite 7 jours avant. 7-3 jours : remboursement 50%. < 3 jours : aucun",
                "Conditions météo dangereuses : report ou remboursement intégral. < 48h : remboursement 50%",
                "Annulation gratuite 24h avant. < 24h : aucun remboursement"
        ));

        // ═══════════════════════════════════════════════════════════════
        //  CONDITIONS D'ANNULATION — Autre
        // ═══════════════════════════════════════════════════════════════
        CONDITIONS.put("Autre", List.of(
                "Annulation gratuite 24h avant. < 24h : aucun remboursement",
                "Aucun remboursement après inscription",
                "Remboursement intégral en cas d'annulation par l'organisateur",
                "Conditions communiquées avant l'activité"
        ));
    }

    /**
     * Retourne la liste des équipements prédéfinis pour la catégorie donnée.
     *
     * @param categorie la catégorie sélectionnée (Sport, Culture, Loisir, Nature, Autre)
     * @return liste d'options d'équipement, ou liste vide si catégorie inconnue
     */
    public List<String> getEquipementOptions(String categorie) {
        if (categorie == null) return Collections.emptyList();
        return EQUIPEMENTS.getOrDefault(categorie, Collections.emptyList());
    }

    /**
     * Retourne la liste des conditions d'annulation prédéfinies pour la catégorie donnée.
     *
     * @param categorie la catégorie sélectionnée
     * @return liste d'options de conditions, ou liste vide si catégorie inconnue
     */
    public List<String> getConditionsOptions(String categorie) {
        if (categorie == null) return Collections.emptyList();
        return CONDITIONS.getOrDefault(categorie, Collections.emptyList());
    }
}
