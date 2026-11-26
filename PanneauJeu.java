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

// PanneauJeu est la scène graphique du jeu (JPanel) et réagit au Timer (ActionListener).
public class PanneauJeu extends JPanel implements ActionListener {

    // Dimensions fixes de la fenêtre et du sol
    public static final int LARGEUR_ECRAN = 480;
    public static final int HAUTEUR_ECRAN = 640;
    public static final int HAUTEUR_SOL = 100;

    // --- ATTRIBUTS DU JEU ---

    private Oiseau oiseau;                     // L'oiseau contrôlé par le joueur
    private ArrayList<PaireTuyaux> tuyaux;     // Liste dynamique des paires de tuyaux actives
    private Random random;                     // Générateur aléatoire pour hauteurs et pièces

    private double solX;                       // Décalage horizontal pour animer le sol
    private int score;                         // Score de la partie en cours
    private String dernierBonusTexte;          // Petit message temporaire ("Parfait ! +2", "+1 Piece !")
    private int compteurMessageBonus;          // Nombre de frames restantes pour afficher le message

    private boolean enAttentePret;             // true avant le premier saut ("Prêt ?")
    private Timer boucleJeu;                   // Minuteur ~60 FPS (16 ms entre chaque image)

    // --- CONSTRUCTEUR ---

    public PanneauJeu() {
        this.random = new Random();
        this.tuyaux = new ArrayList<>();
        initialiserPartie();

        setFocusable(true); // Indispensable pour capter les touches du clavier

        // Écoute de la barre Espace pour sauter / démarrer
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    actionSautOuDemarrage();
                }
            }
        });

        // Écoute du clic souris pour sauter / démarrer
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                actionSautOuDemarrage();
            }
        });

        // Lance la boucle de jeu toutes les 16 ms
        this.boucleJeu = new Timer(16, this);
        this.boucleJeu.start();
    }

    // Prépare ou réinitialise proprement une partie
    private void initialiserPartie() {
        this.oiseau = new Oiseau(110.0, 250.0);
        this.tuyaux.clear();
        this.solX = 0.0;
        this.score = 0;
        this.dernierBonusTexte = "";
        this.compteurMessageBonus = 0;
        this.enAttentePret = true;

        // Crée 3 paires de tuyaux espacées de 220 pixels à droite de l'écran
        for (int i = 0; i < 3; i++) {
            ajouterNouvellePaire(LARGEUR_ECRAN + i * 220);
        }
    }

    // Calcule une hauteur aléatoire pour le tuyau du haut et ajoute une nouvelle paire
    private void ajouterNouvellePaire(double positionX) {
        int hauteurZoneJeu = HAUTEUR_ECRAN - HAUTEUR_SOL; // Espace disponible au-dessus du sol
        int ecartTrou = 150;                               // Hauteur du passage entre haut et bas
        int margeHautBas = 60;                             // Garde une marge pour éviter un trou collé au bord

        // Borne minimale et maximale pour le bas du tuyau supérieur
        int minHaut = margeHautBas;
        int maxHaut = hauteurZoneJeu - ecartTrou - margeHautBas;

        // Hauteur tirée au hasard dans l'intervalle [minHaut, maxHaut]
        int hauteurTuyauHaut = minHaut + this.random.nextInt(Math.max(1, maxHaut - minHaut + 1));

        double vitesseTuyau = 2.5;
        boolean avecPiece = this.random.nextInt(100) < 65; // 65 % de chances de placer une pièce dans le trou

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

    // Démarre la partie si on était sur "Prêt ?" puis fait sauter l'oiseau
    private void actionSautOuDemarrage() {
        if (this.enAttentePret) {
            this.enAttentePret = false;
        }
        this.oiseau.flap();
    }

    // Gère la perte d'une vie après un choc (et réinitialise si 0 vie restante)
    private void gererChocEtReplacer() {
        this.oiseau.perdreVie();

        if (this.oiseau.getVie() <= 0) {
            initialiserPartie();
            return;
        }

        // Replace l'oiseau et régénère les tuyaux un peu plus loin pour éviter un choc instantané
        this.oiseau.reinitialiserPosition(110.0, 230.0);
        this.enAttentePret = true;
        this.tuyaux.clear();
        for (int i = 0; i < 3; i++) {
            ajouterNouvellePaire(LARGEUR_ECRAN + i * 220);
        }
    }

    // --- BOUCLE DE JEU (appelée ~60 fois par seconde par le Timer) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Fait défiler les rayures du sol vers la gauche en boucle infinie
        double vitesseSol = 2.5;
        this.solX = this.solX - vitesseSol;
        if (this.solX <= -24) {
            this.solX = 0.0;
        }

        // 2. Décrémente l'affichage temporaire d'un bonus ("Parfait ! +2" ou "+1 Piece !")
        if (this.compteurMessageBonus > 0) {
            this.compteurMessageBonus--;
            if (this.compteurMessageBonus == 0) {
                this.dernierBonusTexte = "";
            }
        }

        // 3. Mise à jour de la physique et des obstacles si la partie est lancée
        if (!this.enAttentePret) {
            // Applique la gravité et déplace l'oiseau verticalement
            this.oiseau.mettreAJour();

            // Récupère le rectangle de collision de l'oiseau
            Rectangle oiseauBox = this.oiseau.getHitbox();

            // Parcourt toutes les paires de tuyaux actives
            for (int i = 0; i < this.tuyaux.size(); i++) {
                PaireTuyaux paire = this.tuyaux.get(i);
                paire.mettreAJour(); // Avance le tuyau (et sa pièce) vers la gauche

                // Vérifie si l'oiseau touche la pièce dorée au milieu du trou
                Rectangle pieceBox = paire.getHitboxPiece();
                if (!pieceBox.isEmpty() && oiseauBox.intersects(pieceBox)) {
                    paire.ramasserPiece();
                    this.oiseau.gagnerPieces(1);
                    this.dernierBonusTexte = "+1 Piece !";
                    this.compteurMessageBonus = 35;
                }

                // Si le bord droit du tuyau a dépassé l'oiseau et qu'on n'a pas encore compté ce passage
                if (!paire.isPointAttribue() && (paire.getX() + paire.getLargeurTuyau()) < this.oiseau.getX()) {
                    paire.marquerPointAttribue();
                    int pointsGagnes = paire.calculerPointsPassage(this.oiseau);
                    this.score = this.score + pointsGagnes;

                    if (pointsGagnes == 2) {
                        this.dernierBonusTexte = "Parfait ! +2";
                        this.compteurMessageBonus = 45;
                    }
                }

                // Vérifie si l'oiseau heurte le tuyau supérieur ou le tuyau inférieur
                if (oiseauBox.intersects(paire.getHitboxHaut()) || oiseauBox.intersects(paire.getHitboxBas())) {
                    gererChocEtReplacer();
                    break;
                }
            }

            // Si la première paire est totalement sortie à gauche de l'écran, on la retire
            // et on en ajoute une nouvelle à droite après le dernier tuyau
            if (!this.tuyaux.isEmpty() && (this.tuyaux.get(0).getX() + this.tuyaux.get(0).getLargeurTuyau()) < 0) {
                this.tuyaux.remove(0);
                double dernierX = this.tuyaux.get(this.tuyaux.size() - 1).getX();
                ajouterNouvellePaire(dernierX + 220);
            }

            // Vérifie si l'oiseau touche le plafond (y < 0) ou le sol
            int limiteSolY = HAUTEUR_ECRAN - HAUTEUR_SOL - this.oiseau.getHauteur();
            if (this.oiseau.getY() < 0 || this.oiseau.getY() >= limiteSolY) {
                gererChocEtReplacer();
            }
        }

        // Demande à Swing de redessiner l'écran avec les nouvelles positions
        repaint();
    }

    // --- DESSIN DE LA SCÈNE (du fond vers l'avant) ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int ySol = HAUTEUR_ECRAN - HAUTEUR_SOL;

        // 1. Ciel bleu
        g.setColor(new Color(112, 197, 206));
        g.fillRect(0, 0, LARGEUR_ECRAN, ySol);

        // 2. Paires de tuyaux et pièces
        for (PaireTuyaux paire : this.tuyaux) {
            paire.dessiner(g);
        }

        // 3. Sol en bas de l'écran
        g.setColor(new Color(222, 216, 149));
        g.fillRect(0, ySol, LARGEUR_ECRAN, HAUTEUR_SOL);

        // Bande d'herbe verte et rayures défilantes
        g.setColor(new Color(115, 191, 46));
        g.fillRect(0, ySol, LARGEUR_ECRAN, 18);

        g.setColor(new Color(90, 155, 32));
        for (int x = (int) this.solX; x < LARGEUR_ECRAN + 24; x += 24) {
            g.fillRect(x, ySol, 10, 18);
        }

        // 4. Oiseau
        this.oiseau.Dessiner(g);

        // 5. Affichage du score, des vies, des pièces et des messages bonus
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        g.drawString(String.valueOf(this.score), LARGEUR_ECRAN / 2 - 15, 55);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Vies : " + this.oiseau.getVie(), 20, 35);
        g.drawString("Pieces : " + this.oiseau.getPieces(), 20, 58);

        if (!this.dernierBonusTexte.isEmpty()) {
            g.setColor(new Color(255, 245, 120));
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.drawString(this.dernierBonusTexte, LARGEUR_ECRAN / 2 - 55, 95);
        }

        // 6. Écran d'attente avant le premier saut
        if (this.enAttentePret) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            g.drawString("Pret ? (Espace ou Clic)", 88, 185);
        }
    }

    // Lance la fenêtre Swing du jeu
    public static void main(String[] args) {
        JFrame fenetre = new JFrame("Flappy Bird+ - Jour 5");
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