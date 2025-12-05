package de.word_light.document_builder.entites.documentParts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;

import de.word_light.document_builder.abstracts.AbstractEntity;
import de.word_light.document_builder.documentBuilder.TableUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;


/**
 * Wrapper defining the request body that is expected from frontend.
 * 
 * @since 0.0.1
 */
@Entity
@Getter
@Setter
public class DocumentWrapper extends AbstractEntity {

    public static final String FILE_NAME_PATTERN = "^[\\w\\-. ]+.(docx|pdf)$";
    
    @NotNull(message = "'content' cannot be null.")
    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(
        name = "document_wrapper_basic_paragraphs",
        inverseJoinColumns = @JoinColumn(name = "basic_paragraph_id"))
    private List<@Valid @NotNull(message = "'basicParagraph' cannot be null") BasicParagraph> content;

    @NotNull(message = "'tableConfigs' cannot be null.")
    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(
        name = "document_wrapper_table_configs",
        inverseJoinColumns = @JoinColumn(name = "table_config_id"))
    private List<@Valid @NotNull(message = "'tableConfig cannot be null") TableConfig> tableConfigs;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "picture_file_name", unique = true)
    @Column(name = "picture_bytes", length = 16777215)
    @Nullable
    @Schema(hidden = true)
    private Map<String, byte[]> pictures;

    @NotEmpty(message = "'fileName' cannot be empty.")
    // TODO: test this regex
    @Pattern(regexp = FILE_NAME_PATTERN, message = "Wrong format of 'fileName'. Check file format and remove special chars (only - . _ are permitted).")
    @Schema(defaultValue = "document1.docx")
    private String fileName;

    private boolean landscape = false;

    /** Refers to 'Columns' in MS Word */
    @Min(value = 1, message = "'numColumns' too small. Min: 1") 
    @Max(value = 3, message = "'numColumns' too large. Max: 3") 
    @Schema(defaultValue = "1")
    private int numColumns = 1;

    /** Number of lines on top of the first page in one single column ignoring 'numColumns' */
    @Min(value = 0, message = "'numSingleColumnLines' too small. Min: 0") 
    @Schema(defaultValue = "0")
    private int numSingleColumnLines;


    public DocumentWrapper() {
        
        this.content = new ArrayList<>();
        this.tableConfigs = new ArrayList<>();
        this.pictures = new HashMap<>();
    }


    public DocumentWrapper(
            @NotNull(message = "'content' cannot be null.") List<@Valid @NotNull(message = "'basicParagraph' cannot be null") BasicParagraph> content,
            @Valid @NotNull(message = "'tableConfigs' cannot be null.") List<@Valid @NotNull(message = "'tableConfig cannot be null") TableConfig> tableConfigs,
            boolean landscape,
            @NotEmpty(message = "'fileName' cannot be empty.") String fileName, 
            @Min(1) @Max(3) int numColumns,
            @Min(value = 0, message = "'numSingleColumnLines' too small. Min: 0") @Max(value = 5, message = "'numSingleColumnLines' too large. Max: 5") int numSingleColumnLines) {
        
        this.content = content;
        this.tableConfigs = tableConfigs;
        this.pictures = new HashMap<>();
        this.fileName = fileName;
        this.landscape = landscape;
        this.numColumns = numColumns;
        this.numSingleColumnLines = numSingleColumnLines;
    }


    /**
     * @return false if indices of table configs are overlapping
     */
    // NOTE: @AssertTrue method names have to start with 'is'
    @AssertTrue(message = "'tableConfigs' invalid. Start and end indices between tables cannot overlap.")
    @Schema(hidden = true)
    public boolean isTableConfigsNotOverlap() {

        // sort by startIndex
        List<TableConfig> tableConfigs = sortTableConfigsByStartIndex(this.tableConfigs);
        
        for (int i = 0; i < tableConfigs.size(); i++) {
            TableConfig tableConfig = tableConfigs.get(i);

            // case: last tableConfig
            if (i == tableConfigs.size() - 1)
                break;

            TableConfig nextTableConfig = tableConfigs.get(i + 1);

            // case: tableConfigs are overlapping
            if (tableConfig.getEndIndex() >= nextTableConfig.getStartIndex())
                return false;
        }

        return true;
    }


    /**
     * @return false if any table index is out of bounds of content size 
     */
    @AssertTrue(message = "'tableConfigs' invalid. Start and end indices cannot be out of bounds of content size - 1.")
    @Schema(hidden = true)
    public boolean isIndicesNotExceedContentSize() {

        // sort by startIndex
        for (TableConfig tableConfig : tableConfigs) {
            // case: index exceeds content size
            if (tableConfig.getStartIndex() > this.content.size() - 1 || tableConfig.getEndIndex() > this.content.size() - 1)
                return false;
        }

        return true;
    }


    /**
     * @return false if at least one singleColumnLine would be inside a table cell
     */
    @AssertTrue(message = "'tableConfigs' invalid. Cannot put 'singleColumnLine' inside a table.")
    @Schema(hidden = true)
    public boolean isSingleColumnLineNotInsideTable() {

        // case: no singleColumnLines anyway
        if (this.numColumns == 1)
            return true;

        for (TableConfig tableConfig : tableConfigs) {
            // case: singleColumnLine inside table (this only works because singleColumnLines can only start at content index 1)
            for (int i = 1; i <= this.numSingleColumnLines; i++) 
                if (TableUtils.isTableIndex(tableConfig, i))
                    return false;
        }

        return true;
    }


    /**
     * @return false if there's more 'numSingleColumnLines' than lines in total (minus header and footer), else true
     */
    @AssertTrue(message = "'numSingleColumnLines' invalid. Cannot have more singleColumnLines than content size - 2.")
    @Schema(hidden = true)
    public boolean isNumSingleColumnLinesValid() {

        return this.numSingleColumnLines == 0 || this.numSingleColumnLines <= this.content.size() - 2;
    }


    /**
     * @param tableConfigs to sort
     * @return given list by {@code startIndex} ascending
     */
    private List<TableConfig> sortTableConfigsByStartIndex(List<TableConfig> tableConfigs) {

        tableConfigs.sort((TableConfig t1, TableConfig t2) -> {
            return Integer.compare(t1.getStartIndex(), t2.getStartIndex());
        });

        return tableConfigs;
    }
}