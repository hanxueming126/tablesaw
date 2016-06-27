package com.github.lwhite1.tablesaw.table;

import com.github.lwhite1.tablesaw.api.Table;
import com.github.lwhite1.tablesaw.columns.Column;
import com.google.common.annotations.VisibleForTesting;
import org.roaringbitmap.RoaringBitmap;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A group of tables formed by performing splitting operations on an original table
 */
public class ViewGroup implements Iterable<TemporaryView> {

  private final Table sortedOriginal;

  private List<TemporaryView> subTables = new ArrayList<>();

  // the name(s) of the column(s) we're splitting the table on
  private String[] splitColumnNames;

  public ViewGroup(Table original, Column... columns) {
    splitColumnNames = new String[columns.length];
    for (int i = 0; i < columns.length; i++) {
      splitColumnNames[i] = columns[i].name();
    }
    this.sortedOriginal = original.sortOn(splitColumnNames);
    splitOn(splitColumnNames);
  }

  /**
   * Splits the sortedOriginal table into sub-tables, grouping on the columns whose names are given in splitColumnNames
   */
  private void splitOn(String... columnNames) {

    List<Column> columns = sortedOriginal.columns(columnNames);
    int byteSize = 0;
    {
      for (Column c : columns) {
        byteSize += c.byteSize();
      }
    }

    byte[] currentKey = null;
    TemporaryView view;

    RoaringBitmap bitmap = new RoaringBitmap();
    //IntIterator intIterator = sortedOriginal.iterator();
    for (int row = 0; row < sortedOriginal.rowCount(); row++) {
      ByteBuffer byteBuffer = ByteBuffer.allocate(byteSize);
      for (Column c : columns) {
        //System.out.println(c.getString(row));
        //System.out.println(sortedOriginal.get(sortedOriginal.columnIndex(c), row));
        byteBuffer.put(c.asBytes(row));
      }
      byteBuffer.flip();  //TODO(lwhite): Is this needed?
      byte[] newKey = byteBuffer.array();
      //System.out.println(Arrays.toString(newKey));
      if (row == 0) {
        currentKey = newKey;
      }
      if (!Arrays.equals(newKey, currentKey)) {
        currentKey = newKey;
        view = new TemporaryView(sortedOriginal, bitmap);
        subTables.add(view);
        bitmap = new RoaringBitmap();
        bitmap.add(row);
      } else {
        bitmap.add(row);
      }
    }
    if (!bitmap.isEmpty()) {
      view = new TemporaryView(sortedOriginal, bitmap);
      subTables.add(view);
    }
  }

  public List<TemporaryView> getSubTables() {
    return subTables;
  }

  @VisibleForTesting
  public Table getSortedOriginal() {
    return sortedOriginal;
  }

  public int size() {
    return subTables.size();
  }

  /**
   private SubTable splitGroupingColumn(SubTable subTable, List<Column> columnNames) {

   List<Column> newColumns = new ArrayList<>();

   for (Column column : columnNames) {
   Column newColumn = column.emptyCopy();
   newColumns.add(newColumn);
   }
   // iterate through the rows in the table and split each of the grouping columns into multiple columns
   for (int row = 0; row < subTable.rowCount(); row++) {
   List<String> strings = SPLITTER.splitToList(subTable.name());
   for (int col = 0; col < newColumns.size(); col++) {
   newColumns.get(col).addCell(strings.get(col));
   }
   }
   for (Column c : newColumns) {
   subTable.addColumn(c);
   }
   return subTable;
   }

   */
/**
 * Returns an iterator over elements of type {@code T}.
 *
 * @return an Iterator.
 */
  @Override
  public Iterator<TemporaryView> iterator() {
    return subTables.iterator();
  }

  /**
   * Returns the integer as a byte[]
   */
  private byte[] asBytes(int value) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(4).putInt(value);
    byte[] bytes = byteBuffer.array();
    return bytes;
  }

}