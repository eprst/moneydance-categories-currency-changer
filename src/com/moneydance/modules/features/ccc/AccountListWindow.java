
package com.moneydance.modules.features.ccc;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AccountBook;
import com.infinitekind.moneydance.model.CurrencyType;
import com.moneydance.awt.AwtUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class AccountListWindow extends JFrame implements ActionListener {
    private Main extension;
    private DefaultListModel<Account> categoriesListModel;
    private JList<Account> categoriesList;
    private JButton selectButton;
    private JButton changeButton;

    AccountListWindow(Main extension) {
        super("Select Categories to Change");
        this.extension = extension;

        AccountBook book = extension.getUnprotectedContext().getCurrentAccountBook();

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

        JTextField inputArea = new JTextField();
        inputArea.setEditable(true);

        selectButton = new JButton("Select by currency");
        changeButton = new JButton("Change currency");

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(new JScrollPane(categoriesList), AwtUtil.getConstraints(0, 0, 1, 1, 4, 1, true, true));
        p.add(Box.createVerticalStrut(8), AwtUtil.getConstraints(0, 2, 0, 0, 1, 1, false, false));
        p.add(selectButton, AwtUtil.getConstraints(0, 3, 1, 0, 1, 1, false, true));
        p.add(changeButton, AwtUtil.getConstraints(1, 3, 1, 0, 1, 1, false, true));
        getContentPane().add(p);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        enableEvents(WindowEvent.WINDOW_CLOSING);
        selectButton.addActionListener(this);
        changeButton.addActionListener(this);

        setSize(500, 400);
        AwtUtil.centerWindow(this);
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
                    for (Account account : accounts) {
                        account.setCurrencyType(newCurrency);
                    }
                    SwingUtilities.invokeLater(extension::closeConsole);
                }
            }
        }
    }

    public final void processEvent(AWTEvent evt) {
        if (evt.getID() == WindowEvent.WINDOW_CLOSING) {
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
}
