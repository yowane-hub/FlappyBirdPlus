import java.awt.Color;
import java.awt.Graphics;

// Oiseau hérite de ObjetJeu : il possède déjà x, y, largeur, hauteur et leurs getters.
public class Oiseau extends ObjetJeu {

    // Vitesse verticale de l'oiseau (négative = monte, positive = descend)
    private double vitesseY;

    // Nombre de vies restantes dans la partie en cours (3 au début)
    private int vies;

    // Nombre total de pièces possédées par l'oiseau/joueur
    private int pieces;

    // Compteur en "frames" (images du Timer) pour les 2 secondes d'invincibilité :
    // ~60 frames par seconde * 2 secondes = 120 frames.
    // Tant que framesInvincibilite > 0, l'oiseau ne perd pas de vie sur un choc.
    private int framesInvincibilite;

    // Constructeur : initialise l'oiseau à la position (x, y) avec sa taille (34x24)
    public Oiseau(double x, double y) {
        super(x, y, 34, 24);
        this.vitesseY = 0.0;
        this.vies = 3;
        this.pieces = 0;
        this.framesInvincibilite = 0;
    }

    // Retourne le nombre de vies restantes
    public int getVies() {
        return this.vies;
    }

    // Retourne le nombre de pièces
    public int getPieces() {
        return this.pieces;
    }

    // Renvoie true si l'oiseau est actuellement en période d'invincibilité
    public boolean estInvincible() {
        return this.framesInvincibilite > 0;
    }

    // Active 2 secondes d'invincibilité (120 frames à ~60 FPS) après un choc
    public void activerInvincibiliteDeuxSecondes() {
        this.framesInvincibilite = 120;
    }

    // Tente de dépenser un certain nombre de pièces (ex. 20 pièces pour "Continuer").
    // Renvoie true si le paiement a réussi, false si le joueur n'a pas assez de pièces.
    public boolean depenserPieces(int cout) {
        if (cout > 0 && this.pieces >= cout) {
            this.pieces = this.pieces - cout;
            return true;
        }
        return false;
    }

    // Redonne 1 vie (sans dépasser le maximum de 3 vies)
    public void recupererVie() {
        if (this.vies < 3) {
            this.vies = this.vies + 1;
        }
    }

    // Réinitialise l'oiseau pour une toute nouvelle partie (3 vies, invincibilité à 0)
    public void reinitialiserPourNouvellePartie(double x, double y) {
        this.x = x;
        this.y = y;
        this.vitesseY = 0.0;
        this.vies = 3;
        this.framesInvincibilite = 0;
    }

    // Replace l'oiseau après un choc ou lors d'un "Continuer" sans effacer ses pièces
    public void replacerApresChoc(double x, double y) {
        this.x = x;
        this.y = y;
        this.vitesseY = 0.0;
    }

    // Fait sauter l'oiseau vers le haut
    public void flap() {
        this.vitesseY = -8.0;
    }

    // Met à jour le compteur d'invincibilité puis applique la gravité
    @Override
    public void mettreAJour() {
        // Si l'oiseau est invincible, on réduit son compteur de 1 à chaque image
        if (this.framesInvincibilite > 0) {
            this.framesInvincibilite--;
        }

        // Gravité : la vitesse vers le bas augmente, puis la position Y change
        double gravite = 0.5;
        this.vitesseY = this.vitesseY + gravite;
        this.y = this.y + this.vitesseY;
    }

    // Enlève 1 vie si l'oiseau en a encore au moins 1
    public void perdreVie() {
        if (this.vies > 0) {
            this.vies = this.vies - 1;
        }
    }

    // Ajoute des pièces si la quantité est positive
    public void gagnerPieces(int quantite) {
        if (quantite > 0) {
            this.pieces = this.pieces + quantite;
        }
    }

    // Dessine l'oiseau à l'écran
    public void dessiner(Graphics g) {
        // Effet visuel de clignotement pendant les 2 secondes d'invincibilité :
        // toutes les 4 frames, on saute le dessin une fois sur deux.
        if (estInvincible() && (this.framesInvincibilite / 4) % 2 == 0) {
            return;
        }

        int px = (int) this.x;
        int py = (int) this.y;

        // Corps de l'oiseau (jaune vif en temps normal, jaune clair pendant l'invincibilité)
        g.setColor(estInvincible() ? new Color(255, 240, 120) : Color.YELLOW);
        g.fillOval(px, py, this.largeur, this.hauteur);

        // Contour noir
        g.setColor(Color.BLACK);
        g.drawOval(px, py, this.largeur, this.hauteur);

        // Œil blanc et pupille noire
        g.setColor(Color.WHITE);
        g.fillOval(px + this.largeur - 12, py + 5, 7, 7);
        g.setColor(Color.BLACK);
        g.fillOval(px + this.largeur - 9, py + 7, 3, 3);
    }
}