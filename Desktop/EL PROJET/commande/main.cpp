#include "commande.h"

#include <QApplication>

int main(int argc, char *argv[])
{
    QApplication a(argc, argv);
    commande w;
    w.show();
    return a.exec();
}
