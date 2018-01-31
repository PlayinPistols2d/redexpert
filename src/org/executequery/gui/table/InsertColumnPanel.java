package org.executequery.gui.table;

import org.executequery.databasemediators.MetaDataValues;
import org.executequery.databasemediators.QueryTypes;
import org.executequery.databasemediators.spi.DefaultStatementExecutor;
import org.executequery.databaseobjects.DatabaseColumn;
import org.executequery.databaseobjects.DatabaseTable;
import org.executequery.databaseobjects.impl.DatabaseTableColumn;
import org.executequery.gui.ActionContainer;
import org.executequery.gui.ExecuteQueryDialog;
import org.executequery.gui.browser.ColumnData;
import org.executequery.gui.databaseobjects.AbstractCreateObjectPanel;
import org.executequery.gui.datatype.DomainPanel;
import org.executequery.gui.datatype.SelectTypePanel;
import org.executequery.gui.text.SimpleSqlTextPanel;
import org.executequery.gui.text.SimpleTextArea;
import org.executequery.log.Log;
import org.executequery.sql.spi.LiquibaseStatementGenerator;
import org.underworldlabs.util.MiscUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


public class InsertColumnPanel extends AbstractCreateObjectPanel implements KeyListener {
    public static final String CREATE_TITLE = "Insert Column";
    public static final String EDIT_TITLE = "Edit Column";
    private DomainPanel domainPanel;
    private SimpleTextArea defaultValuePanel;
    private SimpleSqlTextPanel checkPanel;
    private SimpleSqlTextPanel computedPanel;
    private SelectTypePanel selectTypePanel;
    private AutoIncrementPanel autoIncrementPanel;
    private JLabel tableLabel;
    private JTextField tableNameField;
    private JCheckBox notNullBox;
    private JCheckBox primaryBox;

    ColumnData columnData;
    DatabaseTable table;
    StringBuffer sb;
    MetaDataValues metaData;
    ActionContainer parent;
    DatabaseColumn columnEdited;
    DatabaseTableColumn column;
    boolean editing;
    LiquibaseStatementGenerator statementGenerator;
    private SimpleSqlTextPanel descriptionPanel;
    private SimpleSqlTextPanel sqlPanel;

    public InsertColumnPanel(DatabaseTable table, ActionContainer dialog) {
        this(table, dialog, null);
    }

    public InsertColumnPanel(DatabaseTable table, ActionContainer dialog, DatabaseColumn column) {
        super(table.getHost().getDatabaseConnection(), dialog, column, new Object[]{table});
    }

    protected void init() {
        String domain = null;
        if (editing)
            domain = columnEdited.getDomain();
        domainPanel = new DomainPanel(columnData, domain);
        defaultValuePanel = new SimpleTextArea();
        checkPanel = new SimpleSqlTextPanel();
        computedPanel = new SimpleSqlTextPanel();
        descriptionPanel = new SimpleSqlTextPanel();
        sqlPanel = new SimpleSqlTextPanel();
        selectTypePanel = new SelectTypePanel(metaData.getDataTypesArray(), metaData.getIntDataTypesArray(), columnData, false);
        autoIncrementPanel = new AutoIncrementPanel(connection, null, columnData.getAutoincrement(), table.getName(), getGenerators());
        tableLabel = new JLabel("Table:");
        tableNameField = new JTextField(table.getName());
        notNullBox = new JCheckBox("Not Null");
        primaryBox = new JCheckBox("Primary Key");

        tableNameField.setEnabled(false);

        nameField.addKeyListener(this);

        notNullBox.addActionListener(actionEvent -> {

            columnData.setNotNull(notNullBox.isSelected());
            if (editing) {
                column.makeCopy();
                column.setRequired(notNullBox.isSelected());
            }
        });
        columnData.setNotNull(notNullBox.isSelected());
        primaryBox.addActionListener(actionEvent -> columnData.setPrimaryKey(primaryBox.isSelected()));

        domainPanel.addDomainComboBoxActionListener(actionEvent -> {
            if (domainPanel.getDomainComboBoxSelectedIndex() != 0)
                selectTypePanel.refresh();
            if (editing) {
                column.makeCopy();
                column.setDomain((String) domainPanel.getDomainComboBoxSelectedItem());
            }
        });

        tabbedPane.addChangeListener(changeEvent -> {
            if (tabbedPane.getSelectedComponent() == sqlPanel) {
                generateSQL();
            }
        });

        defaultValuePanel.addKeyListener(this);
        checkPanel.addKeyListener(this);
        computedPanel.addKeyListener(this);
        descriptionPanel.addKeyListener(this);
        centralPanel.setLayout(new GridBagLayout());
        centralPanel.add(tableLabel, new GridBagConstraints(0, 0,
                1, 1, 0, 0,
                GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5),
                0, 0));
        centralPanel.add(tableNameField, new GridBagConstraints(1, 0,
                1, 1, 1, 0,
                GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5),
                0, 0));
        centralPanel.add(notNullBox, new GridBagConstraints(0, 1,
                1, 1, 0, 0,
                GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5),
                0, 0));
        centralPanel.add(primaryBox, new GridBagConstraints(1, 1,
                1, 1, 0, 0,
                GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5),
                0, 0));
        tabbedPane.add("Domain", domainPanel);
        if (!editing || columnEdited.getDomain().toUpperCase().startsWith("RDB$"))
            tabbedPane.add("Type", selectTypePanel);
        tabbedPane.add("Default Value", defaultValuePanel);
        if (!editing)
            tabbedPane.add("Check", checkPanel);
        if (!editing || !MiscUtils.isNull(columnEdited.getComputedSource()))
            tabbedPane.add("Computed by", computedPanel);
        tabbedPane.add("Autoincrement", autoIncrementPanel);
        tabbedPane.add("Description", descriptionPanel);
        tabbedPane.add("SQL", sqlPanel);
    }

    @Override
    protected void initEdited() {
        try {
            init_edited_elements();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createObject() {
        if (tabbedPane.getSelectedComponent() != sqlPanel)
            generateSQL();
        ExecuteQueryDialog eqd = new ExecuteQueryDialog("Add Column", sqlPanel.getSQLText(), connection, true, "^");
        eqd.display();
        if (eqd.getCommit())
            parent.finished();
    }

    @Override
    public String getCreateTitle() {
        return CREATE_TITLE;
    }

    @Override
    public String getEditTitle() {
        return EDIT_TITLE;
    }

    @Override
    public String getTypeObject() {
        return "DATABASE_TABLE_COLUMN";
    }

    @Override
    public void setDatabaseObject(Object databaseObject) {

        columnEdited = (DatabaseColumn) databaseObject;

    }

    @Override
    public void setParameters(Object[] params) {
        this.table = (DatabaseTable) params[0];
        metaData = new MetaDataValues(true);
        metaData.setDatabaseConnection(connection);
        columnData = new ColumnData(connection);
        sb = new StringBuffer(200);
        this.column = new DatabaseTableColumn(table, columnEdited);
        statementGenerator = new LiquibaseStatementGenerator();
    }

    void init_edited_elements() {
        columnData.setSQLType(column.getTypeInt());
        columnData.setColumnType(column.getTypeName());
        columnData.setColumnSize(column.getColumnSize());
        columnData.setColumnScale(column.getColumnScale());
        columnData.setColumnSubtype(column.getColumnSubtype());
        selectTypePanel.refresh();
        nameField.setText(columnEdited.getName());
        notNullBox.setSelected(columnEdited.isRequired());
        if (getDatabaseVersion() < 3)
            notNullBox.setEnabled(false);
        defaultValuePanel.getTextAreaComponent().setText(columnEdited.getDefaultValue() != null ? columnEdited.getDefaultValue() : "");
        computedPanel.setSQLText(columnEdited.getComputedSource() != null ? columnEdited.getComputedSource() : "");
        descriptionPanel.setSQLText(columnEdited.getRemarks() != null ? columnEdited.getRemarks() : "");
        /*if(!MiscUtils.isNull(columnEdited.getComputedSource()))
        {
            notNullBox.setEnabled(false);
        }*/
        primaryBox.setEnabled(false);
    }


    @Override
    public void keyTyped(KeyEvent keyEvent) {

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {

    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        if (keyEvent.getSource() == defaultValuePanel) {
            columnData.setDefaultValue(defaultValuePanel.getTextAreaComponent().getText());
            if (editing) {
                column.makeCopy();
                column.setDefaultValue(defaultValuePanel.getTextAreaComponent().getText());
            }
        } else if (keyEvent.getSource() == checkPanel) {
            columnData.setCheck(checkPanel.getSQLText());
        } else if (keyEvent.getSource() == computedPanel) {
            columnData.setComputedBy(computedPanel.getSQLText());
            if (editing) {
                column.makeCopy();
                column.setComputedSource(computedPanel.getSQLText());
            }
        } else if (keyEvent.getSource() == descriptionPanel) {
            columnData.setDescription(descriptionPanel.getSQLText());
            if (editing) {
                column.makeCopy();
                column.setColumnDescription(descriptionPanel.getSQLText());
            }
        } else if (keyEvent.getSource() == nameField) {
            columnData.setColumnName(nameField.getText());
            if (editing) {
                column.makeCopy();
                column.setName(nameField.getText());
            }
        }

    }

    String[] getGenerators() {
        DefaultStatementExecutor executor = new DefaultStatementExecutor(connection, true);
        List<String> domains = new ArrayList<>();
        try {
            String query = "select " +
                    "RDB$GENERATOR_NAME FROM RDB$GENERATORS " +
                    "where RDB$SYSTEM_FLAG = 0 " +
                    "order by 1";
            ResultSet rs = executor.execute(QueryTypes.SELECT, query).getResultSet();
            while (rs.next()) {
                domains.add(rs.getString(1).trim());
            }
            executor.releaseResources();
            return domains.toArray(new String[domains.size()]);
        } catch (Exception e) {
            Log.error("Error loading generators:" + e.getMessage());
            return null;
        }
    }


    void generateSQL() {
        sb.setLength(0);
        if (editing) {
            column.makeCopy();
            column.setTypeInt(columnData.getSQLType());
            column.setTypeName(columnData.getColumnType());
            column.setColumnSize(columnData.getColumnSize());
            column.setColumnScale(columnData.getColumnScale());
            sb.append(statementGenerator.alterColumn(column, table).replace(";", "^"));
            if (columnData.isAutoincrement()) {
                sb.append(columnData.getAutoincrement().getSqlAutoincrement());
            }
            sqlPanel.setSQLText(sb.toString());
        } else {
            sb.append("ALTER TABLE ").append(table.getName()).append("\nADD ").append(columnData.getColumnName()).append("\n");
            if (MiscUtils.isNull(columnData.getComputedBy())) {
                if (MiscUtils.isNull(columnData.getDomain())) {
                    if (columnData.getColumnType() != null) {
                        sb.append(columnData.getFormattedDataType());
                    }
                } else {
                    sb.append(columnData.getDomain());
                }
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
                    sb.append(" DEFAULT ").append(value);
                }
                sb.append(columnData.isRequired() ? " NOT NULL" : "");
                if (!MiscUtils.isNull(columnData.getCheck())) {
                    sb.append(" CHECK ( ").append(columnData.getCheck()).append(")");
                }
            } else sb.append("COMPUTED BY ( " + columnData.getComputedBy() + ")");
            if (columnData.isPrimaryKey()) {
                sb.append(" PRIMARY KEY");
            }
            sb.append("^");
            if (!MiscUtils.isNull(columnData.getDescription())) {
                sb.append("\nCOMMENT ON COLUMN ").append(table.getName()).append(".").append(columnData.getColumnName()).append(" IS '")
                        .append(columnData.getDescription()).append("'^");
            }
            autoIncrementPanel.generateAI();
            if (columnData.isAutoincrement()) {
                sb.append(columnData.getAutoincrement().getSqlAutoincrement());
            }
            sqlPanel.setSQLText(sb.toString());
        }
    }
}
