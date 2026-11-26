import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

// Représente un obstacle complet : un tuyau du haut, un tuyau du bas, un trou et une pièce
public class PaireTuyaux {
    private double x;
    private int largeurTuyau;
    private int hauteurTuyauHaut; // Bas du tuyau du haut (donc haut du trou)
    private int ecartTrou;       // Hauteur de l'ouverture entre les deux tuyaux
    private int hauteurEcranJeu; // Hauteur de la zone au-dessus du sol
    private double vitesseX;

    private boolean pointAttribue;
    private boolean pieceRamassée;
    private int rayonPiece;

    public PaireTuyaux(double x, int largeurTuyau, int hauteurTuyauHaut, int ecartTrou, int hauteurEcranJeu, double vitesseX, boolean avecPiece) {
        this.x = x;
        this.largeurTuyau = largeurTuyau;
        this.hauteurTuyauHaut = hauteurTuyauHaut;
        this.ecartTrou = ecartTrou;
        this.hauteurEcranJeu = hauteurEcranJeu;
        this.vitesseX = vitesseX;

        this.pointAttribue = false;
        this.pieceRamassée = !avecPiece; // Si pas de pièce, on la considère déjà absente
        this.rayonPiece = 10;
    }

    public double getX() {
        return this.x;
    }

    public int getLargeurTuyau() {
        return this.largeurTuyau;
    }

    public boolean isPointAttribue() {
        return this.pointAttribue;
    }

    public void marquerPointAttribue() {
        this.pointAttribue = true;
    }

    // Déplace la paire de tuyaux vers la gauche
    public void mettreAJour() {
        this.x = this.x - this.vitesseX;
    }

    // Retourne le rectangle du tuyau supérieur (de y = 0 jusqu'à hauteurTuyauHaut)
    public Rectangle getHitboxHaut() {
        return new Rectangle((int) this.x, 0, this.largeurTuyau, this.hauteurTuyauHaut);
    }

    // Retourne le rectangle du tuyau inférieur (du bas du trou jusqu'au sol)
    public Rectangle getHitboxBas() {
        int yBas = this.hauteurTuyauHaut + this.ecartTrou;
        int hauteurBas = Math.max(0, this.hauteurEcranJeu - yBas);
        return new Rectangle((int) this.x, yBas, this.largeurTuyau, hauteurBas);
    }

    // Retourne le rectangle de la pièce placée au centre du trou
    public Rectangle getHitboxPiece() {
        if (this.pieceRamassée) {
            return new Rectangle(0, 0, 0, 0);
        }
        int centreX = (int) this.x + this.largeurTuyau / 2;
        int centreY = this.hauteurTuyauHaut + this.ecartTrou / 2;
        int diametre = this.rayonPiece * 2;
        return new Rectangle(centreX - this.rayonPiece, centreY - this.rayonPiece, diametre, diametre);
    }

    public void ramasserPiece() {
        this.pieceRamassée = true;
    }

    // Calcule les points gagnés quand l'oiseau franchit le tuyau :
    // +2 si passage proche du centre du trou ("passage parfait"), sinon +1
    public int calculerPointsPassage(Oiseau oiseau) {
        double centreOiseauY = oiseau.getY() + oiseau.getHauteur() / 2.0;
        double centreTrouY = this.hauteurTuyauHaut + this.ecartTrou / 2.0;
        double distanceCentre = Math.abs(centreOiseauY - centreTrouY);

        // Tolérance de 18 pixels autour du centre exact du trou pour un passage parfait
        if (distanceCentre <= 18.0) {
            return 2;
        }
        return 1;
    }

    // Dessine le tuyau du haut, le tuyau du bas et la pièce éventuelle
    public void dessiner(Graphics g) {
        int px = (int) this.x;
        int yBas = this.hauteurTuyauHaut + this.ecartTrou;
        int hauteurBas = Math.max(0, this.hauteurEcranJeu - yBas);

        // Tuyau supérieur
        g.setColor(new Color(76, 175, 80));
        g.fillRect(px, 0, this.largeurTuyau, this.hauteurTuyauHaut);
        g.setColor(new Color(46, 125, 50));
        g.drawRect(px, 0, this.largeurTuyau, this.hauteurTuyauHaut);
        // Rebords du tuyau supérieur
        g.setColor(new Color(102, 187, 106));
        g.fillRect(px - 4, this.hauteurTuyauHaut - 20, this.largeurTuyau + 8, 20);

        // Tuyau inférieur
        g.setColor(new Color(76, 175, 80));
        g.fillRect(px, yBas, this.largeurTuyau, hauteurBas);
        g.setColor(new Color(46, 125, 50));
        g.drawRect(px, yBas, this.largeurTuyau, hauteurBas);
        // Rebords du tuyau inférieur
        g.setColor(new Color(102, 187, 106));
        g.fillRect(px - 4, yBas, this.largeurTuyau + 8, 20);

        // Pièce dorée au milieu du trou si non ramassée
        if (!this.pieceRamassée) {
            Rectangle pieceBox = getHitboxPiece();
            g.setColor(new Color(255, 215, 0));
            g.fillOval(pieceBox.x, pieceBox.y, pieceBox.width, pieceBox.height);
            g.setColor(new Color(184, 134, 11));
            g.drawOval(pieceBox.x, pieceBox.y, pieceBox.width, pieceBox.height);
        }
    }
}
