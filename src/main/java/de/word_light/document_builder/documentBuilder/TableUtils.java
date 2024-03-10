package de.word_light.document_builder.documentBuilder;

import java.util.List;

import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import de.word_light.document_builder.entites.documentParts.BasicParagraph;
import de.word_light.document_builder.entites.documentParts.TableConfig;
import de.word_light.document_builder.entites.documentParts.style.Style;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;


/**
 * Util class for adding tables to an {@link XWPFDocument}.
 * 
 * @since 0.0.1
 */
@Getter
@Setter
@Log4j2
public class TableUtils {

    public static final Integer TABLE_CELL_MARGIN = 80;
    public static final Integer TABLE_WIDTH = DocumentBuilder.PAGE_LONG_SIDE_WITH_BORDER / 2;

    private XWPFDocument document;

    private List<TableConfig> tableConfigs;

    // set by createTableParagraph()
    private TableConfig currentTableConfig;

    // set by getCurrentTable()
    private int currentTableIndex;

    // set by createTableParagraph()
    private Style currentTableStyle;

    // set by getTableFromHeaderFooter()
    private boolean doesDocumentHaveHeaderTable;


    public TableUtils(XWPFDocument document, List<TableConfig> tableConfigs) {

        this.document = document;
        this.tableConfigs = tableConfigs;
    }


    /**
     * Uses {@link TableConfig} to get or create a table and returns a new paragraph or an existing one in the current table cell.
     * 
     * @param currentContentIndex index of the {@link #content} element currently processed
     * @param style to apply to table and text
     * @return paragraph in table cell holding text and style information
     */
    XWPFParagraph createTableParagraph(int currentContentIndex, int contentSize, Style style) {

        // set current table config
        this.currentTableConfig = getCurrentTableConfig(currentContentIndex);
        this.currentTableStyle = style;
        
        // create table or use existing one
        XWPFTable currentTable = getCurrentTable(currentContentIndex, contentSize);

        // case: not inside a table
        if (currentTable == null) {
            log.warn("Failed to create table paragraph. 'currentContentIndex':" + currentContentIndex + " is not inside a table.");
            return null;
        }

        // get current row and colum
        int startIndex = this.currentTableConfig.getStartIndex();
        int currentRow = (currentContentIndex - startIndex) / this.currentTableConfig.getNumColumns();
        int currentCol = (currentContentIndex - startIndex) % this.currentTableConfig.getNumColumns();
        
        // create cell in current position
        XWPFTableCell tableCell = currentTable.getRow(currentRow).getCell(currentCol);

        // add paragraph or use existing one
        return tableCell.getParagraphs().isEmpty() ? tableCell.addParagraph() : 
                                                     tableCell.getParagraphs().get(0);
    }
    

    /**
     * Adds content to a single table cell. Adds one {@link BasicParagraph} per cell.
     * 
     * @param text to add to table cell
     * @param style to apply to text
     */
    XWPFParagraph fillTableCell(XWPFParagraph paragraph, String text, Style style) {

        // add text
        paragraph.createRun().setText(text);

        // add style
        new DocumentBuilder().applyStyle(paragraph, style);

        return paragraph;
    }
    

    /**
     * Overloading {@link #isTableIndex(TableConfig, int)} using {@link #getCurrentTable(int, Style)} as
     * {@link TableConfig}.
     * 
     * @param currentContentIndex index of the {@link #content} element currently processed
     * @return true if tableConfig not null and index at a table cell
     */
    boolean isTableIndex(int currentContentIndex) {

        TableConfig tableConfig = getCurrentTableConfig(currentContentIndex);

        return tableConfig != null && isTableIndex(tableConfig, currentContentIndex);
    }


    /**
     * Get {@link XWPFTable} in document related to given {@code currentContentIndex} or create a new one if not
     * exists yet.<p>
     * 
     * Depends on {@link #createTableParagraph()} beeing called first because of some field variables.
     * 
     * @param currentContentIndex index of the {@link #content} element currently processed
     * @param contentSize size of document content (see {@link DocumentBuilder})
     * @return the current {@link XWPFTable} from {@link #document} or null if {@code currentContentIndex} is not inside a table
     */
    private XWPFTable getCurrentTable(int currentContentIndex, int contentSize) {

        this.currentTableIndex = this.currentTableConfig == null ? -1 : this.tableConfigs.indexOf(this.currentTableConfig);

        // case: currently not inside a table
        if (this.currentTableIndex == -1)
            return null;

        XWPFTable currentTable = null;

        // case: first table config
        if (this.currentTableIndex == 0)
            currentTable = getTableFromHeader(currentContentIndex, contentSize, currentContentIndex == 0);
            
        // case: last table config
        if (this.currentTableIndex == this.tableConfigs.size() - 1 && currentTable == null)
            currentTable = getTableFromFooter(currentContentIndex, contentSize, this.currentTableConfig.getEndIndex() == contentSize - 1);

        // case: any table config that is not inside header or footer
        if (currentTable == null)
            currentTable = getTableFromBody(currentContentIndex, contentSize, true);

        return currentTable;
    }


    /**
     * Depends on {@link #createTableParagraph()} beeing called first because of some field variables.
     * 
     * @param currentContentIndex index of the {@link #content} element currently processed
     * @param contentSize size of document content (see {@link DocumentBuilder})
     * @param createNew if true a new table will be created inside the header in case there hasn't been one, if false {@code null} is returned
     * @return an existing {@link XWPFTable} inside the default header or create new one or return null (depending on params)
     */
    private XWPFTable getTableFromHeader(int currentContentIndex, int contentSize, boolean createNew) {

        try {
            // try to get header table
            return this.document.getHeaderFooterPolicy().getDefaultHeader().getTables().get(0);

        // case: no table created yet
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            if (createNew) {
                this.doesDocumentHaveHeaderTable = true;
                return createNewTable(0, contentSize, TABLE_WIDTH);
            }
        
            return null;
        }
    }


    /**
     * Depends on {@link #createTableParagraph()} beeing called first because of some field variables.
     *  
     * @param currentContentIndex index of the {@link #content} element currently processed
     * @param contentSize size of document content (see {@link DocumentBuilder})
     * @param createNew if true a new table will be created inside the body in case there hasn't been one, if false {@code null} is returned
     * @return an existing {@link XWPFTable} inside the document body (not header or footer) or create new one or return null (depending on params)
     */
    private XWPFTable getTableFromBody(int currentContentIndex, int contentSize, boolean createNew) {

        try {
            // try to get body table
            return this.document.getTables().get(this.doesDocumentHaveHeaderTable ? this.currentTableIndex - 1 : this.currentTableIndex);

        // case: no tables in body yet
        } catch (IndexOutOfBoundsException e) {
            if (createNew)
                return createNewTable(currentContentIndex, contentSize, TABLE_WIDTH);

            return null;
        }
    }


    /**
     * Depends on {@link #createTableParagraph()} beeing called first because of some field variables.
     * 
     * @param currentContentIndex index of the {@link #content} element currently processed
     * @param contentSize size of document content (see {@link DocumentBuilder})
     * @param createNew if true a new table will be created inside the footer in case there hasn't been one, if false {@code null} is returned
     * @return an existing {@link XWPFTable} inside the default footer or create new one or return null (depending on params)
     */
    private XWPFTable getTableFromFooter(int currentContentIndex, int contentSize, boolean createNew) {

        try {
            // try to get footer table
            return this.document.getHeaderFooterPolicy().getDefaultFooter().getTables().get(0);

        // case: no table created yet
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            if (createNew)
                return createNewTable(contentSize - 1, contentSize, TABLE_WIDTH);
        
            return null;
        }
    }


    /**
     * Get the {@link TableConfig} from {@link #tableConfigs} list where {@code currentContentIndex} is in between
     * {@code startIndex} and {@code endIndex}. <p>
     * 
     * Depends on {@link #createTableParagraph()} beeing called first because of some field variables.
     * 
     * @param currentContentIndex index of the {@link #content} element currently processed
     * @return the {@link TableConfig} matching the {@code currentContentIndex} or null if no match is found
     */
    private TableConfig getCurrentTableConfig(int currentContentIndex) {

        for (TableConfig tableConfig : this.tableConfigs)
            if (isTableIndex(tableConfig, currentContentIndex))
                return tableConfig;

        return null;
    }


    /**
     * Adds new table to the document and style it. If {@code currentContentIndex} is 0 or {@code contentSize - 1}, add header or
     * footer first and then add table inside header / footer.<p>
     * 
     * Depends on {@link #createTableParagraph()} beeing called first because of some field variables.
     * 
     * @param currentContentIndex index of the {@link #content} element currently processed
     * @param contentSize size of document content (see {@link DocumentBuilder})
     * @param tableWidth width of the table
     * @return newly created {@link XWPFTable}
     */
    private XWPFTable createNewTable(int currentContentIndex, int contentSize, int tableWidth) {

        XWPFTable table;
        // case: table inside header
        if (currentContentIndex == 0) {
            table = this.document.createHeader(HeaderFooterType.DEFAULT).createTable(this.currentTableConfig.getNumRows(), this.currentTableConfig.getNumColumns());
        
        // case: table inside footer
        } else if (currentContentIndex == contentSize - 1) {
            table = this.document.createFooter(HeaderFooterType.DEFAULT).createTable(this.currentTableConfig.getNumRows(), this.currentTableConfig.getNumColumns());
        
        // case: table inside body
        } else
            table = this.document.createTable(this.currentTableConfig.getNumRows(), this.currentTableConfig.getNumColumns());

        // add style
        addTableStyle(tableWidth, table);

        return table;
    }


    /**
     * Add table specific styles to given table.<p>
     * 
     * Depends on {@link #createTableParagraph()} beeing called first because of some field variables.
     * 
     * @param tableWidth width of the table
     * @param table to style
     */
    private void addTableStyle(int tableWidth, XWPFTable table) {

        if (this.currentTableStyle == null || table == null)
            return;

        TableRowAlign tableRowAlign;
            
        // text align, use CENTER as default
        if (this.currentTableStyle.getTextAlign() == ParagraphAlignment.LEFT) {
            tableRowAlign = TableRowAlign.LEFT;

        } else if (this.currentTableStyle.getTextAlign() == ParagraphAlignment.RIGHT) {
            tableRowAlign = TableRowAlign.RIGHT;

        } else
            tableRowAlign = TableRowAlign.CENTER;

        table.setTableAlignment(tableRowAlign);

        // set table cell margins
        table.setCellMargins(TABLE_CELL_MARGIN, TABLE_CELL_MARGIN, TABLE_CELL_MARGIN, TABLE_CELL_MARGIN);    
        
        // set table width
        table.setWidth(tableWidth);
    }    


    /**
     * Checks if given content index is currently inside a table cell in given {@link TableConfig}.
     * 
     * @param tableConfig to check
     * @param currentContentIndex index of the {@link #content} element currently processed
     * @return true if tableConfig not null and index at a table cell
     */
    public static boolean isTableIndex(TableConfig tableConfig, int currentContentIndex) {

        boolean hasTableStarted = currentContentIndex >= tableConfig.getStartIndex();

        boolean hasTableNotEnded = currentContentIndex <= tableConfig.getEndIndex();

        return hasTableStarted && hasTableNotEnded;
    }
}