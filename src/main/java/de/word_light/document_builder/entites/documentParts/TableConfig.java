package de.word_light.document_builder.entites.documentParts;

import de.word_light.document_builder.abstracts.AbstractEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * Class holding necessary table information.
 * 
 * @since 0.0.1
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TableConfig extends AbstractEntity {

    @NotNull(message = "'numColumns' cannot be null.")
    @Min(value = 1, message = "'numColumns' has to be greater than equal 1.")
    private Integer numColumns;
    
    @NotNull(message = "'numRows' cannot be null.")
    @Min(value = 1, message = "'numRows' has to be greater than equal 1.")
    private Integer numRows;
    
    /** The index in content list with the first table element. */
    @NotNull(message = "'startIndex' cannot be null.")
    @Min(value = 0, message = "'startIndex' has to be greater than equal 0.")
    private Integer startIndex;


    /**
     * Calls all neccessary validation methods on fields.
     * 
     * @return true if all fields are valid
     */
    @AssertTrue(message = "Invalid 'tableConfig'. Not enough cells for content.")
    @Schema(hidden = true)
    public boolean isValid() {
        
        return isTableBigEnough();
    }


    /**
     * @return the index in content list with the last table element
     */
    @Schema(hidden = true)
    public int getEndIndex() {

        return this.startIndex + this.numColumns * this.numRows - 1;
    }
    
    
    /**
     * Checks that product of table columns and rows is greater equal than the number of cells that will 
     * actually be filled.
     * 
     * @return true if table has enough cells
     */
    private boolean isTableBigEnough() {
        
        int numTableCells = getNumColumns() * getNumRows();
        int numFilledCells = getEndIndex() - getStartIndex() + 1;
        
        // should have at least as many table cells as input cells
        return numTableCells >= numFilledCells;
    }
}