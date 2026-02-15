#ifndef COMMANDE_H
#define COMMANDE_H

#include <QMainWindow>

QT_BEGIN_NAMESPACE
namespace Ui { class commande; }
QT_END_NAMESPACE

class commande : public QMainWindow
{
    Q_OBJECT

public:
    commande(QWidget *parent = nullptr);
    ~commande();

private:
    Ui::commande *ui;
};
#endif // COMMANDE_H
