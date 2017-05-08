
package com.moneydance.modules.features.ccc;

import com.infinitekind.moneydance.model.*;
import com.moneydance.awt.AwtUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

public class CCCWindow extends JFrame implements ActionListener {
    private Main extension;
    private DefaultListModel<Account> categoriesListModel;
    private JList<Account> categoriesList;
    private JCheckBox fixTransactions = new JCheckBox("Fix transactions");
    private JButton selectButton = new JButton("Select by currency");
    private JButton changeButton = new JButton("Change currency");
    private JProgressBar progressBar = new JProgressBar();
    private JLabel status = new JLabel();

    private Collection<JComponent> controls;
    private volatile boolean inProgress = false;

    CCCWindow(Main extension) {
        super("Select Categories to Change");
        this.extension = extension;

        AccountBook book = extension.getUnprotectedContext().getCurrentAccountBook();

        fixTransactions.setToolTipText("Fix transactions with currency conversion by setting conversion rate to 1.0");

        categoriesListModel = new DefaultListModel<>();
        if (book != null) {
            addSubCategories(book.getRootAccount(), categoriesListModel);
        }

        categoriesList = new JList<>(categoriesListModel);
        categoriesList.setCellRenderer(
                new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        Account acct = (Account) value;
                        StringBuilder sb = new StringBuilder();
                        sb.append(acct.getAccountName()).append("  [").append(acct.getCurrencyType()).append("]");

                        StringBuilder sb2 = new StringBuilder();
                        while (acct.getParentAccount() != null) {
                            sb2.append("  ");
                            acct = acct.getParentAccount();
                        }
                        sb2.append(sb);
                        c.setText(sb2.toString());

                        return c;
                    }
                }
        );

        categoriesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        selectButton.setToolTipText("Select categories by category currency");
        fixTransactions.setSelected(true);
        status.setText("Select categories to change, then click Change currency");

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(new JScrollPane(categoriesList), AwtUtil.getConstraints(0, 0, 1, 1, 4, 1, true, true));
        p.add(Box.createVerticalStrut(8), AwtUtil.getConstraints(0, 2, 0, 0, 1, 1, false, false));
        p.add(fixTransactions, AwtUtil.getConstraints(0, 3, 1, 0, 4, 1, true, false));
        p.add(selectButton, AwtUtil.getConstraints(0, 4, 1, 0, 1, 1, false, true));
        p.add(changeButton, AwtUtil.getConstraints(1, 4, 1, 0, 1, 1, false, true));
        p.add(Box.createVerticalStrut(8), AwtUtil.getConstraints(0, 6, 0, 0, 1, 1, false, false));
        p.add(progressBar, AwtUtil.getConstraints(0, 7, 1, 0, 4, 1, true, false));
        p.add(status, AwtUtil.getConstraints(0, 8, 1, 0, 4, 1, true, false));
        getContentPane().add(p);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        enableEvents(WindowEvent.WINDOW_CLOSING);
        selectButton.addActionListener(this);
        changeButton.addActionListener(this);

        setSize(500, 400);
        AwtUtil.centerWindow(this);

        controls = Arrays.asList(categoriesList, fixTransactions, selectButton, changeButton, progressBar, status);
    }

    private static void addSubCategories(Account parentAcct, DefaultListModel<Account> accounts) {
        int sz = parentAcct.getSubAccountCount();
        for (int i = 0; i < sz; i++) {
            Account acct = parentAcct.getSubAccount(i);
            Account.AccountType accountType = acct.getAccountType();
            if (accountType == Account.AccountType.INCOME || accountType == Account.AccountType.EXPENSE) {
                accounts.addElement(acct);
                addSubCategories(acct, accounts);
            }
        }
    }

    private Set<CurrencyType> findUsedCurrencies() {
        Set<CurrencyType> result = new HashSet<>();
        for (int i = 0; i < categoriesListModel.size(); i++) {
            result.add(categoriesListModel.getElementAt(i).getCurrencyType());
        }
        return result;
    }


    public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();
        if (src == selectButton) {
            CurrencyType currencyType = selectCurrency("Currency of categories to select", findUsedCurrencies());
            if (currencyType != null) {
                for (int i = 0; i < categoriesListModel.size(); i++) {
                    Account account = categoriesListModel.getElementAt(i);
                    if (account.getCurrencyType().equals(currencyType)) {
                        categoriesList.addSelectionInterval(i, i);
                    }
                }
            }
        } else if (src == changeButton) {
            List<Account> accounts = categoriesList.getSelectedValuesList();
            if (!accounts.isEmpty()) {
                CurrencyType newCurrency = selectCurrency("Select New Currency", null);
                if (newCurrency != null) {
                    new Thread(() -> changeCurrency(accounts, newCurrency)).start();
                }
            }
        }
    }

    private void changeCurrency(List<Account> accounts, CurrencyType newCurrency) {
        inProgress = true;
        controls.forEach(c -> c.setEnabled(false));
        for (Account account : accounts) {
            if (!account.getCurrencyType().equals(newCurrency)) {
                status.setText("Changing currency for " + account.getAccountName());
                account.setCurrencyType(newCurrency);
                if (account.syncItem() && fixTransactions.isSelected()) {
                    fixTransactions(account);
                }
            }
        }

        controls.forEach(c -> c.setEnabled(true));
        inProgress = false;
        SwingUtilities.invokeLater(() -> extension.closeConsole());
    }

    public final void processEvent(AWTEvent evt) {
        if (evt.getID() == WindowEvent.WINDOW_CLOSING && !inProgress) {
            extension.closeConsole();
            return;
        }
        super.processEvent(evt);
    }

    void goAway() {
        setVisible(false);
        dispose();
    }

    private CurrencyType selectCurrency(String title, Set<CurrencyType> filter) {

        List<CurrencyType> allCurrencies = new ArrayList<>(
                extension.getUnprotectedContext().getCurrentAccountBook().getCurrencies().getAllCurrencies()
        );

        if (filter != null)
            allCurrencies.retainAll(filter);

        allCurrencies.removeIf(currencyType -> currencyType.getCurrencyType() != CurrencyType.Type.CURRENCY);

        allCurrencies.sort(Comparator.comparing(CurrencyType::toString));

        JComboBox<CurrencyType> comboBox = new JComboBox<>(allCurrencies.toArray(new CurrencyType[0]));

        if (JOptionPane.showOptionDialog(
                this,
                comboBox,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                null) == JOptionPane.CLOSED_OPTION)
            return null;

        return (CurrencyType) comboBox.getSelectedItem();

        // decides to show it as a list -- ugly

//        return (CurrencyType) JOptionPane.showInputDialog(
//                this,
//                comboBox,
//                title,
//                JOptionPane.QUESTION_MESSAGE,
//                null,
//                allCurrencies.toArray(),
//                allCurrencies.get(0)
//        );

    }

    private void fixTransactions(Account account) {
        AccountBook book = extension.getUnprotectedContext().getCurrentAccountBook();
        TxnSet transactions = book.getTransactionSet().getTransactionsForAccount(account);

        progressBar.setMaximum(transactions.getSize());
        int i = 0;

        for (AbstractTxn txn : transactions) {
            progressBar.setValue(i++);

            if (txn instanceof SplitTxn) {
                SplitTxn splitTxn = (SplitTxn) txn;

                if (splitTxn.getParentTxn().getSplitCount() == 1) {

                    long samt = txn.getValue();
                    long pamt = splitTxn.getParentAmount();

                    if (samt != -pamt /*splitTxn.getRate() != 1.0d*/) {
                        long date = txn.getDateInt();
                        System.err.printf("fixing samt: %d, pamt: %d (%f) date: %s | %s%n", samt, pamt, splitTxn.getRate(), new Date(date), splitTxn.toString());

                        splitTxn.setAmount(-pamt, -pamt);
                        splitTxn.getParentTxn().syncItem();
                    }
                }

            }

        }

    }
}
