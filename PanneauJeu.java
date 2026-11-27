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

// PanneauJeu gère l'affichage, les entrées clavier/souris et la boucle de jeu via Timer
public class PanneauJeu extends JPanel implements ActionListener {

    // Dimensions de la fenêtre, hauteur du sol et coût en pièces de l'option "Continuer"
    public static final int LARGEUR_ECRAN = 480;
    public static final int HAUTEUR_ECRAN = 640;
    public static final int HAUTEUR_SOL = 100;
    public static final int COUT_CONTINUER = 20;

    // Enumération des différents modes/états possibles de la partie
    private enum EtatPartie {
        PRET,       // Écran d'attente avant le premier saut
        JEU,        // Partie en cours (gravité, tuyaux, score, collisions actifs)
        PAUSE,      // Jeu mis en pause par le joueur
        GAME_OVER   // Fin de partie quand les 3 vies sont épuisées (0 vie)
    }

    // --- ATTRIBUTS DE LA PARTIE ---

    private EtatPartie etat;                 // État actuel de la partie
    private Oiseau oiseau;                   // Objet Oiseau du joueur
    private ArrayList<PaireTuyaux> tuyaux;   // Liste des obstacles (tuyaux + pièces)
    private Random random;                   // Générateur aléatoire

    private double solX;                     // Décalage du sol pour l'animation
    private int score;                       // Score actuel
    private int meilleurScore;               // Meilleur score enregistré pendant l'exécution
    private int piecesGagneesPartie;         // Pièces ramassées durant la partie actuelle

    // Variables pour le compte à rebours "3 - 2 - 1" lors de la reprise après pause
    private boolean repriseEnCours;          // true pendant les 3 secondes de compte à rebours
    private int framesCompteRebours;         // 180 frames ≈ 3 secondes (60 frames/sec * 3)

    // Petit message affiché temporairement en haut de l'écran ("Parfait ! +2", "+1 Piece !", etc.)
    private String dernierBonusTexte;
    private int compteurMessageBonus;

    // Rectangles cliquables sur l'écran Game Over
    private Rectangle boutonContinuer;
    private Rectangle boutonRejouer;

    // Boucle principale du jeu (~60 fois par seconde, toutes les 16 ms)
    private Timer boucleJeu;

    // --- CONSTRUCTEUR ---

    public PanneauJeu() {
        this.random = new Random();
        this.tuyaux = new ArrayList<>();
        this.meilleurScore = 0;

        // Définition des zones des deux boutons de l'écran Game Over
        this.boutonContinuer = new Rectangle(115, 415, 250, 44);
        this.boutonRejouer = new Rectangle(115, 470, 250, 44);

        // Prépare la première partie en mode PRET
        demarrerNouvellePartie();

        // Permet au panneau de recevoir les touches du clavier
        setFocusable(true);

        // Écouteur clavier :
        // - ESPACE : sauter ou rejouer si Game Over
        // - P ou ECHAP : mettre en pause ou reprendre avec compte à rebours 3-2-1
        // - C : utiliser l'option Continuer (20 pièces) sur l'écran Game Over
        // - R : rejouer immédiatement sur l'écran Game Over
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                if (code == KeyEvent.VK_SPACE) {
                    if (etat == EtatPartie.GAME_OVER) {
                        demarrerNouvellePartie();
                    } else if (!repriseEnCours) {
                        actionSautOuDemarrage();
                    }
                } else if (code == KeyEvent.VK_P || code == KeyEvent.VK_ESCAPE) {
                    basculerPause();
                } else if (code == KeyEvent.VK_C && etat == EtatPartie.GAME_OVER) {
                    tenterContinuerAvecPieces();
                } else if (code == KeyEvent.VK_R && etat == EtatPartie.GAME_OVER) {
                    demarrerNouvellePartie();
                }
            }
        });

        // Écouteur souris :
        // - Sur Game Over : vérifie si on clique sur "Continuer" ou "Rejouer"
        // - Sinon : fait sauter / démarrer la partie
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int mx = e.getX();
                int my = e.getY();

                if (etat == EtatPartie.GAME_OVER) {
                    if (boutonContinuer.contains(mx, my)) {
                        tenterContinuerAvecPieces();
                    } else if (boutonRejouer.contains(mx, my)) {
                        demarrerNouvellePartie();
                    }
                } else if (!repriseEnCours) {
                    actionSautOuDemarrage();
                }
            }
        });

        // Démarre la boucle de jeu
        this.boucleJeu = new Timer(16, this);
        this.boucleJeu.start();
    }

    // Réinitialise toutes les valeurs pour recommencer une partie complète
    private void demarrerNouvellePartie() {
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

        // Crée 3 paires de tuyaux initiales espacées de 220 pixels
        for (int i = 0; i < 3; i++) {
            ajouterNouvellePaire(LARGEUR_ECRAN + i * 220);
        }
    }

    // Permet de continuer après un Game Over en dépensant 20 pièces pour récupérer 1 vie
    private void tenterContinuerAvecPieces() {
        if (this.oiseau.depenserPieces(COUT_CONTINUER)) {
            this.oiseau.recupererVie();
            this.oiseau.replacerApresChoc(110.0, 230.0);
            this.oiseau.activerInvincibiliteDeuxSecondes();

            // Replace de nouveaux tuyaux à droite pour éviter un choc immédiat
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

    // Passe de JEU à PAUSE, ou déclenche le compte à rebours 3-2-1 depuis PAUSE
    private void basculerPause() {
        if (this.etat == EtatPartie.JEU && !this.repriseEnCours) {
            this.etat = EtatPartie.PAUSE;
        } else if (this.etat == EtatPartie.PAUSE) {
            this.repriseEnCours = true;
            this.framesCompteRebours = 180; // 3 secondes
        }
    }

    // Génère une paire de tuyaux à la position horizontale "positionX"
    private void ajouterNouvellePaire(double positionX) {
        int hauteurZoneJeu = HAUTEUR_ECRAN - HAUTEUR_SOL;
        int ecartTrou = 150;
        int margeHautBas = 60;

        int minHaut = margeHautBas;
        int maxHaut = hauteurZoneJeu - ecartTrou - margeHautBas;
        int hauteurTuyauHaut = minHaut + this.random.nextInt(Math.max(1, maxHaut - minHaut + 1));

        double vitesseTuyau = 2.5;
        boolean avecPiece = this.random.nextInt(100) < 65;

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

    // Lance la partie si on était en PRET, puis fait sauter l'oiseau
    private void actionSautOuDemarrage() {
        if (this.etat == EtatPartie.PRET) {
            this.etat = EtatPartie.JEU;
        }
        if (this.etat == EtatPartie.JEU) {
            this.oiseau.flap();
        }
    }

    // Gère la perte d'une vie quand l'oiseau touche un obstacle ou les limites
    private void gererChoc() {
        this.oiseau.perdreVie();

        // Met à jour le meilleur score si nécessaire
        if (this.score > this.meilleurScore) {
            this.meilleurScore = this.score;
        }

        // S'il ne reste plus de vie (0 vie), on affiche l'écran GAME_OVER
        if (this.oiseau.getVies() <= 0) {
            this.etat = EtatPartie.GAME_OVER;
            return;
        }

        // Sinon, on active 2 secondes d'invincibilité et on replace l'oiseau proprement
        this.oiseau.activerInvincibiliteDeuxSecondes();
        this.oiseau.replacerApresChoc(110.0, 230.0);
        this.dernierBonusTexte = "Choc ! Invincible 2s";
        this.compteurMessageBonus = 50;
    }

    // Calcule le nom de la médaille selon le score obtenu
    private String obtenirMedaille(int scoreFinal) {
        if (scoreFinal >= 100) {
            return "PLATINE";
        } else if (scoreFinal >= 50) {
            return "OR";
        } else if (scoreFinal >= 25) {
            return "ARGENT";
        } else if (scoreFinal >= 10) {
            return "BRONZE";
        }
        return "AUCUNE";
    }

    // Boucle de mise à jour appelée par le Timer toutes les 16 ms
    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Si le compte à rebours 3-2-1 après une pause est actif, on le décrémente
        if (this.repriseEnCours) {
            this.framesCompteRebours--;
            if (this.framesCompteRebours <= 0) {
                this.repriseEnCours = false;
                this.etat = EtatPartie.JEU;
            }
            repaint();
            return;
        }

        // 2. Si le jeu est en PAUSE ou en GAME_OVER, on ne fait pas avancer la physique
        if (this.etat == EtatPartie.PAUSE || this.etat == EtatPartie.GAME_OVER) {
            repaint();
            return;
        }

        // 3. Défilement du sol vers la gauche en boucle
        double vitesseSol = 2.5;
        this.solX = this.solX - vitesseSol;
        if (this.solX <= -24) {
            this.solX = 0.0;
        }

        // 4. Gestion de la durée d'affichage du message bonus
        if (this.compteurMessageBonus > 0) {
            this.compteurMessageBonus--;
            if (this.compteurMessageBonus == 0) {
                this.dernierBonusTexte = "";
            }
        }

        // 5. Logique active uniquement lorsque la partie est en cours (EtatPartie.JEU)
        if (this.etat == EtatPartie.JEU) {
            // Met à jour l'invincibilité et la gravité de l'oiseau
            this.oiseau.mettreAJour();
            Rectangle oiseauBox = this.oiseau.getHitbox();

            // Parcours des tuyaux et pièces
            for (int i = 0; i < this.tuyaux.size(); i++) {
                PaireTuyaux paire = this.tuyaux.get(i);
                paire.mettreAJour();

                // Ramassage de pièce
                Rectangle pieceBox = paire.getHitboxPiece();
                if (!pieceBox.isEmpty() && oiseauBox.intersects(pieceBox)) {
                    paire.ramasserPiece();
                    this.oiseau.gagnerPieces(1);
                    this.piecesGagneesPartie++;
                    this.dernierBonusTexte = "+1 Piece !";
                    this.compteurMessageBonus = 35;
                }

                // Attribution des points (+1 normal ou +2 passage parfait)
                if (!paire.isPointAttribue() && (paire.getX() + paire.getLargeurTuyau()) < this.oiseau.getX()) {
                    paire.marquerPointAttribue();
                    int pointsGagnes = paire.calculerPointsPassage(this.oiseau);
                    this.score = this.score + pointsGagnes;
                    if (this.score > this.meilleurScore) {
                        this.meilleurScore = this.score;
                    }

                    if (pointsGagnes == 2) {
                        this.dernierBonusTexte = "Parfait ! +2";
                        this.compteurMessageBonus = 45;
                    }
                }

                // Collision avec tuyaux uniquement si l'oiseau n'est pas invincible
                if (!this.oiseau.estInvincible()
                        && (oiseauBox.intersects(paire.getHitboxHaut()) || oiseauBox.intersects(paire.getHitboxBas()))) {
                    gererChoc();
                    break;
                }
            }

            // Génération continue de tuyaux à droite quand un tuyau sort à gauche
            if (!this.tuyaux.isEmpty() && (this.tuyaux.get(0).getX() + this.tuyaux.get(0).getLargeurTuyau()) < 0) {
                this.tuyaux.remove(0);
                double dernierX = this.tuyaux.get(this.tuyaux.size() - 1).getX();
                ajouterNouvellePaire(dernierX + 220);
            }

            // Collision avec plafond ou sol si l'oiseau n'est pas invincible
            int limiteSolY = HAUTEUR_ECRAN - HAUTEUR_SOL - this.oiseau.getHauteur();
            if (this.oiseau.getY() < 0) {
                this.oiseau.replacerApresChoc(this.oiseau.getX(), 0.0);
                if (!this.oiseau.estInvincible()) {
                    gererChoc();
                }
            } else if (this.oiseau.getY() >= limiteSolY) {
                this.oiseau.replacerApresChoc(this.oiseau.getX(), limiteSolY);
                if (!this.oiseau.estInvincible()) {
                    gererChoc();
                }
            }
        }

        // Redessine l'écran
        repaint();
    }

    // Dessine la scène et les différents écrans selon l'état
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int ySol = HAUTEUR_ECRAN - HAUTEUR_SOL;

        // 1. Ciel
        g.setColor(new Color(112, 197, 206));
        g.fillRect(0, 0, LARGEUR_ECRAN, ySol);

        // 2. Tuyaux et pièces
        for (PaireTuyaux paire : this.tuyaux) {
            paire.dessiner(g);
        }

        // 3. Sol et rayures
        g.setColor(new Color(222, 216, 149));
        g.fillRect(0, ySol, LARGEUR_ECRAN, HAUTEUR_SOL);

        g.setColor(new Color(115, 191, 46));
        g.fillRect(0, ySol, LARGEUR_ECRAN, 18);

        g.setColor(new Color(90, 155, 32));
        for (int x = (int) this.solX; x < LARGEUR_ECRAN + 24; x += 24) {
            g.fillRect(x, ySol, 10, 18);
        }

        // 4. Oiseau
        this.oiseau.dessiner(g);

        // 5. Affichage du score, des 3 cœurs de vie et des pièces
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        g.drawString(String.valueOf(this.score), LARGEUR_ECRAN / 2 - 15, 55);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        StringBuilder coeurs = new StringBuilder("Vies : ");
        for (int i = 0; i < 3; i++) {
            coeurs.append(i < this.oiseau.getVies() ? "♥ " : "♡ ");
        }
        g.drawString(coeurs.toString(), 20, 35);
        g.drawString("Pieces : " + this.oiseau.getPieces(), 20, 58);

        // Message bonus temporaire
        if (!this.dernierBonusTexte.isEmpty()) {
            g.setColor(new Color(255, 245, 120));
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString(this.dernierBonusTexte, LARGEUR_ECRAN / 2 - 85, 92);
        }

        // 6. Superposition "Prêt ?"
        if (this.etat == EtatPartie.PRET) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            g.drawString("Pret ? (Espace ou Clic)", 88, 185);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString("Pause : touche P", 180, 212);
        }

        // 7. Superposition Pause ou Compte à rebours 3-2-1
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

        // 8. Superposition Game Over
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

            // Bouton Continuer
            g.setColor(this.oiseau.getPieces() >= COUT_CONTINUER ? new Color(235, 165, 25) : Color.GRAY);
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

    // Lance la fenêtre du jeu
    public static void main(String[] args) {
        JFrame fenetre = new JFrame("Flappy Bird+ - Jour 6");
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