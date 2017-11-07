package org.executequery.gui.databaseobjects;

import org.executequery.databasemediators.DatabaseConnection;
import org.executequery.databasemediators.MetaDataValues;
import org.executequery.databaseobjects.DatabaseColumn;
import org.executequery.databaseobjects.DatabaseHost;
import org.executequery.databaseobjects.DatabaseMetaTag;
import org.executequery.databaseobjects.DatabaseTable;
import org.executequery.databaseobjects.impl.*;
import org.executequery.gui.ActionContainer;
import org.executequery.gui.ExecuteQueryDialog;
import org.executequery.gui.browser.ColumnData;
import org.executequery.gui.table.AutoIncrementPanel;
import org.executequery.gui.table.SelectTypePanel;
import org.executequery.gui.text.SQLTextPane;
import org.underworldlabs.util.MiscUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.Types;

import static org.executequery.databaseobjects.NamedObject.DOMAIN;
import static org.executequery.databaseobjects.NamedObject.META_TYPES;

public class CreateDomainPanel extends JPanel implements KeyListener {
    private JLabel connectionLabel;
    private JLabel fieldLabel;
    private JTabbedPane tabPane;
    private JScrollPane scrollDefaultValue;
    private JScrollPane scrollCheck;
    private JScrollPane scrollDescription;
    private JScrollPane scrollSQL;
    private SQLTextPane defaultValueTextPane;
    private SQLTextPane checkTextPane;
    private SQLTextPane sqlTextPane;
    private JTextPane descriptionTextPane;
    private JPanel defaultValuePanel;
    private JPanel checkPanel;
    private JPanel descriptionPanel;
    private JPanel upPanel;
    private JPanel sqlPanel;
    private SelectTypePanel selectTypePanel;
    private JCheckBox notNullBox;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField fieldNameField;

    public static final String TITLE = "Create Domain";

    DatabaseConnection databaseConnection;
    ColumnData columnData;
    ActionContainer parent;
    MetaDataValues metaData;
    boolean editing;
    String domain;
    DefaultDatabaseDomain databaseDomain;
    DatabaseHost host;
    DatabaseTableColumn column;

    public CreateDomainPanel(DatabaseConnection connection, ActionContainer parent, String domain) {
        databaseConnection = connection;
        this.parent = parent;
        this.domain = domain;
        metaData = new MetaDataValues(databaseConnection, true);
        columnData = new ColumnData(databaseConnection);
        editing = domain != null;
        init();
        if(editing)
            init_edited();
    }

    public CreateDomainPanel(DatabaseConnection connection, ActionContainer parent) {
        this(connection, parent, null);
    }

    void init() {
        upPanel = new JPanel();
        defaultValuePanel = new JPanel();
        checkPanel = new JPanel();
        descriptionPanel = new JPanel();
        sqlPanel = new JPanel();
        selectTypePanel = new SelectTypePanel(metaData.getDataTypesArray(), metaData.getIntDataTypesArray(), columnData);
        tabPane = new JTabbedPane();
        fieldLabel = new JLabel("Name:");
        fieldNameField = new JTextField(15);
        notNullBox = new JCheckBox("Not Null");
        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");
        scrollDefaultValue = new JScrollPane();
        scrollCheck = new JScrollPane();
        scrollDescription = new JScrollPane();
        scrollSQL = new JScrollPane();
        defaultValueTextPane = new SQLTextPane();
        checkTextPane = new SQLTextPane();
        sqlTextPane = new SQLTextPane();
        descriptionTextPane = new JTextPane();

        scrollDefaultValue.setViewportView(defaultValueTextPane);

        scrollCheck.setViewportView(checkTextPane);

        scrollDescription.setViewportView(descriptionTextPane);

        scrollSQL.setViewportView(sqlTextPane);

        fieldNameField.addKeyListener(this);

        notNullBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

                columnData.setNotNull(notNullBox.isSelected());
            }
        });
        tabPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                selectTypePanel.refreshColumn();
                if (tabPane.getSelectedComponent() == sqlPanel) {
                    generateSQL();
                }
            }
        });
        defaultValueTextPane.addKeyListener(this);
        checkTextPane.addKeyListener(this);
        descriptionTextPane.addKeyListener(this);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (tabPane.getSelectedComponent() != sqlPanel)
                    generateSQL();
                ExecuteQueryDialog eqd = new ExecuteQueryDialog("Add Domain", sqlTextPane.getText(), databaseConnection, true);
                eqd.display();
                if (eqd.getCommit())
                    parent.finished();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                parent.finished();
            }
        });


        this.setLayout(new GridBagLayout());
        upPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 2, 2), 0, 0);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        upPanel.add(fieldLabel, gbc);
        gbc.gridx++;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        upPanel.add(fieldNameField, gbc);
        gbc.gridx++;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        upPanel.add(notNullBox, gbc);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        defaultValuePanel.setLayout(new GridBagLayout());
        checkPanel.setLayout(new GridBagLayout());
        descriptionPanel.setLayout(new GridBagLayout());
        sqlPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbcFull = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
        defaultValuePanel.add(scrollDefaultValue, gbcFull);
        checkPanel.add(scrollCheck, gbcFull);
        descriptionPanel.add(scrollDescription, gbcFull);
        sqlPanel.add(scrollSQL, gbcFull);
        tabPane.add("Type", selectTypePanel);
        tabPane.add("Default Value", defaultValuePanel);
        tabPane.add("Check", checkPanel);
        tabPane.add("Description", descriptionPanel);
        tabPane.add("SQL", sqlPanel);
        tabPane.setPreferredSize(new Dimension(700, 400));
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.BOTH;
        this.add(upPanel, gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        this.add(tabPane, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JPanel(), gbc);
        gbc.weightx = 0.1;
        gbc.gridx++;
        this.add(okButton, gbc);
        gbc.gridx++;
        this.add(cancelButton, gbc);
    }

    void init_edited() {
        DatabaseMetaTag metaTag = new DefaultDatabaseMetaTag(host, null, null, META_TYPES[DOMAIN]);
        databaseDomain = new DefaultDatabaseDomain(metaTag, domain);
        columnData.setDomain(domain);
        selectTypePanel.refresh();
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {

    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {

        if (keyEvent.getSource() == defaultValueTextPane) {
            columnData.setDefaultValue(defaultValueTextPane.getText());
            if (editing) {
                column.makeCopy();
                column.setDefaultValue(defaultValueTextPane.getText());
            }
        } else if (keyEvent.getSource() == checkTextPane) {
            columnData.setCheck(checkTextPane.getText());
        } else if (keyEvent.getSource() == descriptionTextPane) {
            columnData.setDescription(descriptionTextPane.getText());
            if (editing) {
                column.makeCopy();
                column.setColumnDescription(descriptionTextPane.getText());
            }
        } else if (keyEvent.getSource() == fieldNameField) {
            columnData.setColumnName(fieldNameField.getText());
            if (editing) {
                column.makeCopy();
                column.setName(fieldNameField.getText());
            }
        }

    }

    void generateSQL() {
        StringBuffer sb = new StringBuffer();
        /*if (editing) {
            column.makeCopy();
            column.setTypeInt(columnData.getSQLType());
            column.setTypeName(columnData.getColumnType());
            column.setColumnSize(columnData.getColumnSize());
            column.setColumnScale(columnData.getColumnScale());
            sb.append(statementGenerator.alterColumn(column, table).replace(";", "^"));
            if (columnData.isAutoincrement()) {
                sb.append(columnData.getAutoincrement().getSqlAutoincrement());
            }
            sqlTextPane.setText(sb.toString());
        } else */
        {
            sb.append("CREATE DOMAIN ").append(columnData.getColumnName()).append(" as ").append(columnData.getFormattedDataType()).append("\n");
            if (!MiscUtils.isNull(columnData.getDefaultValue())) {
                String value = "";
                boolean str = false;
                int sqlType = columnData.getSQLType();
                switch (sqlType) {

                    case Types.LONGVARCHAR:
                    case Types.LONGNVARCHAR:
                    case Types.CHAR:
                    case Types.NCHAR:
                    case Types.VARCHAR:
                    case Types.NVARCHAR:
                    case Types.CLOB:
                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        value = "'";
                        str = true;
                        break;
                    default:
                        break;
                }
                value += columnData.getDefaultValue();
                if (str) {
                    value += "'";
                }
                sb.append(" DEFAULT " + value);
            }
            sb.append(columnData.isRequired() ? " NOT NULL" : "");
            if (!MiscUtils.isNull(columnData.getCheck())) {
                sb.append(" CHECK ( " + columnData.getCheck() + ")");
            }
            sb.append(";");
            if (!MiscUtils.isNull(columnData.getDescription())) {
                sb.append("\nCOMMENT ON DOMAIN ").append(columnData.getColumnName()).append(" IS '")
                        .append(columnData.getDescription()).append("'^");
            }
            sqlTextPane.setText(sb.toString());
        }
    }
}
