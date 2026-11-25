import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class PanneauJeu extends JPanel implements ActionListener {

    // Valeurs fixes partagées de la scènes du jeu
    public static final int LARGEUR_ECRAN = 480;
    public static final int HAUTEUR_ECRAN = 640;
    public static final int HAUTEUR_SOL = 100;

    // Attributs
    private Oiseau oiseau; //Objet oiseau
    private double solX; //Position horizontale pour faire défilé les rayure du sol 
    private boolean enAttentePret; //Vrai tant que le joueur n'a pas encore effectuer son 1er saut
    private Timer boucleJeu; //Minuteur du boucle du jeu

    // Constructeur
    public PanneauJeu() {
        this.oiseau = new Oiseau(110.0, 250.0); //Position initiale de l'oiseau

        //etat initiale du sol et d'écran d'attente
        this.solX = 0.0;
        this.enAttentePret = true;

        setFocusable(true); //Permet au panneau de recevoir les évenement du clavier

        // Contrôle par touche Espace
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    actionSautOuDemarrage();
                }
            }
        });

        // Contrôle par clic souris (pratique aussi pour tester rapidement)
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                actionSautOuDemarrage();
            }
        });

        // Boucle de jeu 
        this.boucleJeu = new Timer(16, this);
        this.boucleJeu.start();
    }

    // Comportement interne
    private void actionSautOuDemarrage() {

        // Si on était sur l'écran "Prêt ?", on lance officiellement la partie
        if (this.enAttentePret) {
            this.enAttentePret = false;
        }

        this.oiseau.flap();//L'oiseau effectue un saut
    }

    // Mise à jour du jeu apppelé automatiquement par le timer
    @Override
    public void actionPerformed(ActionEvent e) {
        // Défilement du sol vers la gauche
        double vitesseSol = 2.5;
        this.solX = this.solX - vitesseSol;
        if (this.solX <= -24) {
            this.solX = 0.0;
        }

        // Si le jeu a démarré, mise à jour de l'oiseau
        if (!this.enAttentePret) {
            this.oiseau.mettreAJour();

            // Plafond : on bloque l'oiseau en haut de l'écran
            if (this.oiseau.getY() < 0) {
                this.oiseau.reinitialiserPosition(this.oiseau.getX(), 0.0);
            }

            // Sol : on pose l'oiseau sur le sol pour ce test du Jour 4
            double limiteSolY = HAUTEUR_ECRAN - HAUTEUR_SOL - this.oiseau.getHauteur();
            if (this.oiseau.getY() > limiteSolY) {
                this.oiseau.reinitialiserPosition(this.oiseau.getX(), limiteSolY);
            }
        }

        //Demande à Swing de rappeler paintComponent(g) pour afficher la nouvelle image
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 1. Ciel bleu
        g.setColor(new Color(112, 197, 206));
        g.fillRect(0, 0, LARGEUR_ECRAN, HAUTEUR_ECRAN - HAUTEUR_SOL);

        // 2. Sol en bas
        int ySol = HAUTEUR_ECRAN - HAUTEUR_SOL;
        g.setColor(new Color(222, 216, 149));
        g.fillRect(0, ySol, LARGEUR_ECRAN, HAUTEUR_SOL);

        // Bande verte en haut du sol
        g.setColor(new Color(115, 191, 46));
        g.fillRect(0, ySol, LARGEUR_ECRAN, 18);

        // Rayures qui défilent sur la bande verte
        g.setColor(new Color(90, 155, 32));
        for (int x = (int) this.solX; x < LARGEUR_ECRAN + 24; x += 24) {
            g.fillRect(x, ySol, 10, 18);
        }

        // 3. Oiseau
        this.oiseau.Dessiner(g);

        // 4. Message "Prêt ?" avant le premier saut
        if (this.enAttentePret) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 28));
            g.drawString("Pret ? (Espace ou Clic)", 85, 180);
        }
    }

    public static void main(String[] args) {
        JFrame fenetre = new JFrame("Flappy Bird+ - Jour 4");
        fenetre.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fenetre.setResizable(false);

        PanneauJeu panneau = new PanneauJeu();
        panneau.setPreferredSize(new java.awt.Dimension(LARGEUR_ECRAN, HAUTEUR_ECRAN));
        fenetre.add(panneau);

        fenetre.pack(); // Adapte exactement la fenêtre à la taille du panneau
        fenetre.setLocationRelativeTo(null);
        fenetre.setVisible(true);
        panneau.requestFocusInWindow();
    }
}