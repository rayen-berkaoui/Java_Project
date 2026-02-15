#include "commande.h"
#include "ui_commande.h"

commande::commande(QWidget *parent)
    : QMainWindow(parent)
    , ui(new Ui::commande)
{
    ui->setupUi(this);
}

commande::~commande()
{
    delete ui;
}

