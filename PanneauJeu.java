import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

// PanneauJeu représente la zone de dessin (JPanel) et réagit au Timer (ActionListener)
public class PanneauJeu extends JPanel implements ActionListener {

    // Constantes globales de taille de fenêtre, hauteur du sol et coût pour continuer
    public static final int LARGEUR_ECRAN = 480;
    public static final int HAUTEUR_ECRAN = 640;
    public static final int HAUTEUR_SOL = 100;
    public static final int COUT_CONTINUER = 20;

    // Liste fermée des états possibles de la partie
    private enum EtatPartie {
        PRET,       // Écran d'attente avant le premier saut de l'oiseau
        JEU,        // Partie active : gravité, tuyaux, score, pièces et collisions en cours
        PAUSE,      // Partie mise en pause (ou en compte à rebours 3-2-1 de reprise)
        GAME_OVER   // Partie terminée lorsque l'oiseau n'a plus de vie (0 vie)
    }

    // --- ATTRIBUTS D'ÉTAT ET DE PROGRESSION ---

    private EtatPartie etat;                 // État actuel de la partie
    private Niveau niveauActuel;             // Paramètres du niveau en cours (thème, vitesse, écart)
    private int tuyauxPassesNiveau;          // Nombre de tuyaux franchis dans le niveau actuel

    // --- OBJETS DU JEU ---

    private Oiseau oiseau;                   // L'oiseau contrôlé par le joueur
    private ArrayList<PaireTuyaux> tuyaux;   // Liste dynamique des paires de tuyaux actives
    private Random random;                   // Générateur de nombres aléatoires

    // --- SCORE, SOL ET MESSAGES ---

    private double solX;                     // Position horizontale utilisée pour faire défiler le sol
    private int score;                       // Score de la partie en cours
    private int meilleurScore;               // Meilleur score réalisé durant cette exécution
    private int piecesGagneesPartie;         // Nombre de pièces ramassées durant la partie actuelle

    private boolean repriseEnCours;          // true pendant le compte à rebours 3-2-1 après une pause
    private int framesCompteRebours;         // Nombre de frames restantes pour la reprise (~180 = 3s)

    private String dernierBonusTexte;          // Texte temporaire ("Parfait ! +2", "+1 Piece !", etc.)
    private int compteurMessageBonus;          // Durée restante d'affichage du message bonus

    // --- BOUTONS CLIQUABLES ET BOUCLE DE JEU ---

    private Rectangle boutonContinuer;         // Zone cliquable "Continuer" sur l'écran Game Over
    private Rectangle boutonRejouer;           // Zone cliquable "Rejouer" sur l'écran Game Over
    private Timer boucleJeu;                   // Minuteur appelé toutes les 16 ms (~60 images/sec)

    // --- CONSTRUCTEUR ---

    public PanneauJeu() {
        this.random = new Random();
        this.tuyaux = new ArrayList<>();
        this.meilleurScore = 0;

        // Position et taille des deux boutons affichés sur l'écran Game Over
        this.boutonContinuer = new Rectangle(115, 415, 250, 44);
        this.boutonRejouer = new Rectangle(115, 470, 250, 44);

        // Prépare la première partie dès l'ouverture de la fenêtre
        demarrerNouvellePartie();

        // Autorise le panneau à recevoir les événements du clavier
        setFocusable(true);

        // Écoute des touches du clavier
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                // Touche ESPACE : relance si Game Over, sinon fait sauter/démarrer
                if (code == KeyEvent.VK_SPACE) {
                    if (etat == EtatPartie.GAME_OVER) {
                        demarrerNouvellePartie();
                    } else {
                        if (!repriseEnCours) {
                            actionSautOuDemarrage();
                        }
                    }
                } else if (code == KeyEvent.VK_P || code == KeyEvent.VK_ESCAPE) {
                    // Touche P ou ECHAP : met en pause ou lance la reprise 3-2-1
                    basculerPause();
                } else if (code == KeyEvent.VK_C && etat == EtatPartie.GAME_OVER) {
                    // Touche C sur Game Over : tente de continuer avec 20 pièces
                    tenterContinuerAvecPieces();
                } else if (code == KeyEvent.VK_R && etat == EtatPartie.GAME_OVER) {
                    // Touche R sur Game Over : redémarre une nouvelle partie
                    demarrerNouvellePartie();
                }
            }
        });

        // Écoute des clics de souris
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Redonne le focus clavier au panneau après un clic souris
                requestFocusInWindow();
                int mx = e.getX();
                int my = e.getY();

                // Sur l'écran Game Over, vérifie si l'utilisateur clique sur un bouton
                if (etat == EtatPartie.GAME_OVER) {
                    if (boutonContinuer.contains(mx, my)) {
                        tenterContinuerAvecPieces();
                    } else {
                        if (boutonRejouer.contains(mx, my)) {
                            demarrerNouvellePartie();
                        }
                    }
                } else {
                    // Hors Game Over, un clic fait démarrer ou sauter l'oiseau
                    if (!repriseEnCours) {
                        actionSautOuDemarrage();
                    }
                }
            }
        });

        // Démarre le Timer toutes les 16 ms (~60 fois par seconde)
        this.boucleJeu = new Timer(16, this);
        this.boucleJeu.start();
    }

    // --- MÉTHODES DE GESTION DE PARTIE ET DE NIVEAU ---

    // Réinitialise complètement la partie au Niveau 1 avec 3 vies et score à 0
    private void demarrerNouvellePartie() {
        this.niveauActuel = Niveau.pourNumero(1);
        this.tuyauxPassesNiveau = 0;

        this.oiseau = new Oiseau(110.0, 250.0);
        this.oiseau.reinitialiserPourNouvellePartie(110.0, 250.0);

        this.tuyaux.clear();
        this.solX = 0.0;
        this.score = 0;
        this.piecesGagneesPartie = 0;

        this.repriseEnCours = false;
        this.framesCompteRebours = 0;
        this.dernierBonusTexte = "";
        this.compteurMessageBonus = 0;
        this.etat = EtatPartie.PRET;

        // Crée 3 paires de tuyaux de départ espacées de 220 pixels
        for (int i = 0; i < 3; i++) {
            ajouterNouvellePaire(LARGEUR_ECRAN + i * 220);
        }
    }

    // Passe au niveau suivant lorsque le joueur a franchi tous les tuyaux de l'objectif
    private void passerAuNiveauSuivant() {
        int prochainNumero = this.niveauActuel.getNumero() + 1;
        this.niveauActuel = Niveau.pourNumero(prochainNumero);
        this.tuyauxPassesNiveau = 0;

        this.dernierBonusTexte = "Niveau " + this.niveauActuel.getNumero() + " : " + this.niveauActuel.getNomDecor();
        this.compteurMessageBonus = 75;
    }

    // Permet de continuer après un Game Over en dépensant 20 pièces pour récupérer 1 vie
    private void tenterContinuerAvecPieces() {
        if (this.oiseau.depenserPieces(COUT_CONTINUER)) {
            this.oiseau.recupererVie();
            this.oiseau.replacerApresChoc(110.0, 230.0);
            this.oiseau.activerInvincibiliteDeuxSecondes();

            // Régénère des tuyaux à droite afin de ne pas heurter un obstacle tout de suite
            this.tuyaux.clear();
            for (int i = 0; i < 3; i++) {
                ajouterNouvellePaire(LARGEUR_ECRAN + i * 220);
            }

            this.etat = EtatPartie.JEU;
            this.dernierBonusTexte = "1 Vie recuperee !";
            this.compteurMessageBonus = 45;
        } else {
            this.dernierBonusTexte = "Pas assez de pieces (20 requis)";
            this.compteurMessageBonus = 50;
        }
    }

    // Active la pause si on joue, ou lance le compte à rebours 3-2-1 si on est en pause
    private void basculerPause() {
        if (this.etat == EtatPartie.JEU && !this.repriseEnCours) {
            this.etat = EtatPartie.PAUSE;
        } else {
            if (this.etat == EtatPartie.PAUSE && !this.repriseEnCours) {
                this.repriseEnCours = true;
                this.framesCompteRebours = 180; // 3 secondes à ~60 frames/sec
            }
        }
    }

    // Crée une nouvelle paire de tuyaux à la coordonnée horizontale "positionX"
    private void ajouterNouvellePaire(double positionX) {
        int hauteurZoneJeu = HAUTEUR_ECRAN - HAUTEUR_SOL;
        int ecartTrou = this.niveauActuel.getEcartTrou();
        int margeHautBas = 60;

        // Calcule les limites possibles pour la hauteur du tuyau du haut
        int minHaut = margeHautBas;
        int maxHaut = hauteurZoneJeu - ecartTrou - margeHautBas;
        int amplitude = Math.max(1, maxHaut - minHaut + 1);
        int hauteurTuyauHaut = minHaut + this.random.nextInt(amplitude);

        double vitesseTuyau = this.niveauActuel.getVitesse();
        boolean avecPiece = this.random.nextInt(100) < 65; // 65 % de chance d'avoir une pièce

        PaireTuyaux paire = new PaireTuyaux(
            positionX,
            64,
            hauteurTuyauHaut,
            ecartTrou,
            hauteurZoneJeu,
            vitesseTuyau,
            avecPiece
        );
        this.tuyaux.add(paire);
    }

    // Démarre la partie si elle était en attente ("PRET"), puis fait sauter l'oiseau
    private void actionSautOuDemarrage() {
        if (this.etat == EtatPartie.PRET) {
            this.etat = EtatPartie.JEU;
        }
        if (this.etat == EtatPartie.JEU) {
            this.oiseau.flap();
        }
    }

    // Gère un choc : retire 1 vie, vérifie le Game Over ou active 2s d'invincibilité
    private void gererChoc() {
        this.oiseau.perdreVie();

        if (this.score > this.meilleurScore) {
            this.meilleurScore = this.score;
        }

        // Si plus aucune vie, la partie passe en GAME_OVER
        if (this.oiseau.getVies() <= 0) {
            this.etat = EtatPartie.GAME_OVER;
            return;
        }

        // Sinon, l'oiseau devient invincible pendant 2 secondes et est replacé proprement
        this.oiseau.activerInvincibiliteDeuxSecondes();
        this.oiseau.replacerApresChoc(110.0, 230.0);
        this.dernierBonusTexte = "Choc ! Invincible 2s";
        this.compteurMessageBonus = 50;
    }

    // Détermine la médaille obtenue selon le score final
    private String obtenirMedaille(int scoreFinal) {
        if (scoreFinal >= 100) {
            return "PLATINE";
        } else {
            if (scoreFinal >= 50) {
                return "OR";
            } else {
                if (scoreFinal >= 25) {
                    return "ARGENT";
                } else {
                    if (scoreFinal >= 10) {
                        return "BRONZE";
                    } else {
                        return "AUCUNE";
                    }
                }
            }
        }
    }

    // --- BOUCLE PRINCIPALE DE MISE À JOUR (appelée à chaque tic du Timer) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Si le compte à rebours 3-2-1 de reprise est actif, on le fait défiler
        if (this.repriseEnCours) {
            this.framesCompteRebours--;
            if (this.framesCompteRebours <= 0) {
                this.repriseEnCours = false;
                this.etat = EtatPartie.JEU;
            }
            repaint();
            return;
        }

        // 2. Si le jeu est en PAUSE ou en GAME_OVER, on ne déplace plus les éléments
        if (this.etat == EtatPartie.PAUSE || this.etat == EtatPartie.GAME_OVER) {
            repaint();
            return;
        }

        // 3. Défilement continu du sol vers la gauche selon la vitesse du niveau
        double vitesseSol = this.niveauActuel.getVitesse();
        this.solX = this.solX - vitesseSol;
        if (this.solX <= -24) {
            this.solX = 0.0;
        }

        // 4. Réduction progressive de la durée du message bonus à l'écran
        if (this.compteurMessageBonus > 0) {
            this.compteurMessageBonus--;
            if (this.compteurMessageBonus == 0) {
                this.dernierBonusTexte = "";
            }
        }

        // 5. Mise à jour de la physique, des tuyaux, du score et des collisions en mode JEU
        if (this.etat == EtatPartie.JEU) {
            // Met à jour la gravité et le compteur d'invincibilité de l'oiseau
            this.oiseau.mettreAJour();
            Rectangle oiseauBox = this.oiseau.getHitbox();

            // Parcourt toutes les paires de tuyaux
            for (int i = 0; i < this.tuyaux.size(); i++) {
                PaireTuyaux paire = this.tuyaux.get(i);
                paire.mettreAJour();

                // Vérifie si l'oiseau touche la pièce dorée au milieu du trou
                Rectangle pieceBox = paire.getHitboxPiece();
                if (!pieceBox.isEmpty() && oiseauBox.intersects(pieceBox)) {
                    paire.ramasserPiece();
                    this.oiseau.gagnerPieces(1);
                    this.piecesGagneesPartie++;
                    this.dernierBonusTexte = "+1 Piece !";
                    this.compteurMessageBonus = 35;
                }

                // Vérifie si l'oiseau vient de dépasser le bord droit du tuyau
                boolean depasseTuyau = (paire.getX() + paire.getLargeurTuyau()) < this.oiseau.getX();
                if (!paire.isPointAttribue() && depasseTuyau) {
                    paire.marquerPointAttribue();

                    // +2 points si passage proche du centre du trou, sinon +1 point
                    int pointsGagnes = paire.calculerPointsPassage(this.oiseau);
                    this.score = this.score + pointsGagnes;

                    if (this.score > this.meilleurScore) {
                        this.meilleurScore = this.score;
                    }

                    if (pointsGagnes == 2) {
                        this.dernierBonusTexte = "Parfait ! +2";
                        this.compteurMessageBonus = 45;
                    }

                    // Met à jour la progression des tuyaux du niveau courant
                    this.tuyauxPassesNiveau++;
                    if (this.tuyauxPassesNiveau >= this.niveauActuel.getTuyauxObjectif()) {
                        passerAuNiveauSuivant();
                    }
                }

                // Vérifie les collisions avec le tuyau du haut ou du bas (si non invincible)
                boolean toucheTuyau = oiseauBox.intersects(paire.getHitboxHaut())
                    || oiseauBox.intersects(paire.getHitboxBas());
                if (!this.oiseau.estInvincible() && toucheTuyau) {
                    gererChoc();
                    break;
                }
            }

            // Si la première paire de tuyaux est sortie à gauche, on la retire et on en ajoute une à droite
            if (!this.tuyaux.isEmpty()) {
                PaireTuyaux premierePaire = this.tuyaux.get(0);
                if ((premierePaire.getX() + premierePaire.getLargeurTuyau()) < 0) {
                    this.tuyaux.remove(0);
                    double dernierX = this.tuyaux.get(this.tuyaux.size() - 1).getX();
                    ajouterNouvellePaire(dernierX + 220);
                }
            }

            // Vérifie les collisions avec le plafond (y < 0) et le sol
            int limiteSolY = HAUTEUR_ECRAN - HAUTEUR_SOL - this.oiseau.getHauteur();
            if (this.oiseau.getY() < 0) {
                this.oiseau.replacerApresChoc(this.oiseau.getX(), 0.0);
                if (!this.oiseau.estInvincible()) {
                    gererChoc();
                }
            } else {
                if (this.oiseau.getY() >= limiteSolY) {
                    this.oiseau.replacerApresChoc(this.oiseau.getX(), limiteSolY);
                    if (!this.oiseau.estInvincible()) {
                        gererChoc();
                    }
                }
            }
        }

        // Redessine le panneau avec les nouvelles positions et informations
        repaint();
    }

    // --- DESSIN GRAPHIQUE DE LA SCÈNE ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int ySol = HAUTEUR_ECRAN - HAUTEUR_SOL;

        // 1. Ciel coloré selon le thème du niveau actuel
        g.setColor(this.niveauActuel.getCouleurCiel());
        g.fillRect(0, 0, LARGEUR_ECRAN, ySol);

        // 2. Tuyaux et pièces
        for (PaireTuyaux paire : this.tuyaux) {
            paire.dessiner(g);
        }

        // 3. Sol, bande d'herbe et rayures défilantes
        g.setColor(this.niveauActuel.getCouleurSol());
        g.fillRect(0, ySol, LARGEUR_ECRAN, HAUTEUR_SOL);

        g.setColor(this.niveauActuel.getCouleurHerbe());
        g.fillRect(0, ySol, LARGEUR_ECRAN, 18);

        g.setColor(this.niveauActuel.getCouleurHerbe().darker());
        for (int x = (int) this.solX; x < LARGEUR_ECRAN + 24; x += 24) {
            g.fillRect(x, ySol, 10, 18);
        }

        // 4. Oiseau
        this.oiseau.dessiner(g);

        // 5. Barre d'informations (HUD) : score, vies, pièces et niveau
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        g.drawString(String.valueOf(this.score), LARGEUR_ECRAN / 2 - 15, 55);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        StringBuilder coeurs = new StringBuilder("Vies : ");
        for (int i = 0; i < 3; i++) {
            if (i < this.oiseau.getVies()) {
                coeurs.append("♥ ");
            } else {
                coeurs.append("♡ ");
            }
        }
        g.drawString(coeurs.toString(), 20, 35);
        g.drawString("Pieces : " + this.oiseau.getPieces(), 20, 58);

        g.drawString("NIV " + this.niveauActuel.getNumero(), LARGEUR_ECRAN - 95, 35);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(
            this.tuyauxPassesNiveau + "/" + this.niveauActuel.getTuyauxObjectif() + " tuyaux",
            LARGEUR_ECRAN - 115,
            55
        );

        // Message temporaire de bonus ou de passage de niveau
        if (!this.dernierBonusTexte.isEmpty()) {
            g.setColor(new Color(255, 245, 120));
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString(this.dernierBonusTexte, 75, 92);
        }

        // 6. Écran d'attente "Prêt ?" avant le premier saut
        if (this.etat == EtatPartie.PRET) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            g.drawString("Pret ? (Espace ou Clic)", 88, 185);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString(this.niveauActuel.getNomDecor() + " | Pause : P", 118, 212);
        }

        // 7. Écran de pause ou compte à rebours 3-2-1
        if (this.etat == EtatPartie.PAUSE) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRect(0, 0, LARGEUR_ECRAN, HAUTEUR_ECRAN);

            g.setColor(Color.WHITE);
            if (this.repriseEnCours) {
                int chiffre = Math.max(1, (this.framesCompteRebours + 59) / 60);
                g.setFont(new Font("SansSerif", Font.BOLD, 56));
                g.drawString(String.valueOf(chiffre), LARGEUR_ECRAN / 2 - 18, 290);
                g.setFont(new Font("SansSerif", Font.PLAIN, 18));
                g.drawString("Reprise...", LARGEUR_ECRAN / 2 - 42, 330);
            } else {
                g.setFont(new Font("SansSerif", Font.BOLD, 34));
                g.drawString("PAUSE", LARGEUR_ECRAN / 2 - 62, 260);
                g.setFont(new Font("SansSerif", Font.PLAIN, 18));
                g.drawString("Appuyez sur P pour reprendre (3-2-1)", 85, 305);
            }
        }

        // 8. Écran récapitulatif Game Over
        if (this.etat == EtatPartie.GAME_OVER) {
            g.setColor(new Color(0, 0, 0, 165));
            g.fillRect(0, 0, LARGEUR_ECRAN, HAUTEUR_ECRAN);

            g.setColor(new Color(245, 232, 190));
            g.fillRoundRect(75, 140, 330, 395, 20, 20);
            g.setColor(new Color(120, 85, 40));
            g.drawRoundRect(75, 140, 330, 395, 20, 20);

            g.setColor(new Color(190, 40, 40));
            g.setFont(new Font("SansSerif", Font.BOLD, 32));
            g.drawString("GAME OVER", 145, 190);

            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.drawString("Score : " + this.score, 110, 240);
            g.drawString("Meilleur : " + this.meilleurScore, 110, 272);
            g.drawString("Medaille : " + obtenirMedaille(this.score), 110, 304);
            g.drawString("Pieces gagnees : +" + this.piecesGagneesPartie, 110, 336);

            // Bouton Continuer (actif en doré si le joueur a au moins 20 pièces)
            if (this.oiseau.getPieces() >= COUT_CONTINUER) {
                g.setColor(new Color(235, 165, 25));
            } else {
                g.setColor(Color.GRAY);
            }
            g.fillRoundRect(boutonContinuer.x, boutonContinuer.y, boutonContinuer.width, boutonContinuer.height, 10, 10);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.drawString("Continuer (" + COUT_CONTINUER + " pieces) [C]", 128, 443);

            // Bouton Rejouer
            g.setColor(new Color(76, 175, 80));
            g.fillRoundRect(boutonRejouer.x, boutonRejouer.y, boutonRejouer.width, boutonRejouer.height, 10, 10);
            g.setColor(Color.WHITE);
            g.drawString("Rejouer [Espace / R]", 152, 498);
        }
    }

    // Point d'entrée qui crée la fenêtre Swing et affiche PanneauJeu
    public static void main(String[] args) {
        JFrame fenetre = new JFrame("Flappy Bird+ - Jour 7");
        fenetre.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fenetre.setResizable(false);

        PanneauJeu panneau = new PanneauJeu();
        panneau.setPreferredSize(new Dimension(LARGEUR_ECRAN, HAUTEUR_ECRAN));

        fenetre.add(panneau);
        fenetre.pack();
        fenetre.setLocationRelativeTo(null);
        fenetre.setVisible(true);
        panneau.requestFocusInWindow();
    }
}