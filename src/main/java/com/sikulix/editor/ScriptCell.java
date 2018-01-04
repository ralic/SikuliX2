/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.editor;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.core.SX;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ScriptCell {

  enum CellType {COMMAND, IMAGE, SCRIPT, VARIABLE, LIST, MAP, IMAGELIST, IMAGEMAP, TEXT}

  private Script script;
  private String value = "";
  private CellType cellType = CellType.TEXT;
  private int row = -1;
  private int col = -1;
  private int indentLevel = 0;

  protected ScriptCell(Script script) {
    this.script = script;
    value = "";
  }

  protected ScriptCell(Script script, String value) {
    this.script = script;
    this.value = value.trim();
  }

  protected ScriptCell(Script script, String value, int col) {
    this.script = script;
    this.value = value.trim();
  }

  protected String getIndentMarker() {
    String indent = "";
    if (indentLevel == 1) {
      indent = ">";
    } else if (indentLevel == 2) {
      indent = ">>";
    } else if (indentLevel > 2) {
      indent = ">>!";
    }
    return indent;
  }

  protected void setIndent(int level) {
    if (level > -1) {
      indentLevel = level;
    }
  }

  protected int getIndent() {
    return indentLevel;
  }

  void doIndent() {
    indentLevel++;
  }

  void doDedent() {
    indentLevel = Math.max(0, --indentLevel);
    if (isBlock() && indentLevel == 0) {
      indentLevel = 1;
    }
  }

  private boolean block = false;

  void setBlock() {
    block = true;
  }

  boolean isBlock() {
    return block;
  }

  protected ScriptCell asCommand(int row, int col) {
    if (!value.startsWith("#")) {
      value = "#" + value;
    }
    cellType = CellType.COMMAND;
    return this;
  }

  protected boolean isCommand() {
    return CellType.COMMAND.equals(cellType);
  }

  protected ScriptCell asImage() {
    if (isEmpty() || "@".equals(value)) {
      value = "@?";
      imagename = "";
    } else if (!value.startsWith("@")) {
      imagename = value;
      value = "@?" + value;
    } else {
      imagename = value.replace("@", "").replace("?", "").trim();
    }
    cellType = CellType.IMAGE;
    return this;
  }

  protected boolean isImage() {
    return CellType.IMAGE.equals(cellType);
  }

  String imagename = "";
  Picture picture = null;

  private void getImage() {
    imagename = value.replace("@", "").replace("?", "");
    imagename = Do.input("Image Capture", "... enter a name", imagename);
    if (SX.isSet(imagename)) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          script.getWindow().setVisible(false);
          SX.pause(1);
          picture = Do.userCapture();
          if (SX.isNotNull(picture)) {
            picture.save(imagename, script.getScriptPath().getParent());
          } else {
            imagename = "?" + imagename;
          }
          value = "@" + imagename;
          script.getTable().setValueAt(value, row, col);
          script.getWindow().setVisible(true);
        }
      }).start();
    }
  }

  protected void capture() {
    asImage().getImage();
  }

  protected Element getCellClick() {
    return Script.getCellClick(row, col + 1, script.getWindow(), script.getTable());
  }

  protected void select() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        Do.on().clickFast(getCellClick());
      }
    }).start();
  }

  protected void show() {
    asImage();
    if (isValid()) {
      loadPicture();
      if (SX.isNotNull(picture)) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            picture.show(1);
            Do.on().clickFast(getCellClick());
          }
        }).start();
      } else {
        value = "@?" + imagename;
        script.getTable().setValueAt(value, row, col);
      }
    }
  }

  protected void find() {
    asImage();
    if (isValid()) {
      loadPicture();
      if (SX.isNotNull(picture)) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            script.getWindow().setVisible(false);
            Do.find(picture);
            Do.on().showMatch();
            script.getWindow().setVisible(true);
          }
        }).start();
      } else {
        value = "@?" + imagename;
        script.getTable().setValueAt(value, row, col);
      }
    }
  }

  private void loadPicture() {
    if (SX.isNull(picture)) {
      File fPicture = new File(script.getScriptPath().getParentFile(), imagename + ".png");
      if (fPicture.exists()) {
        picture = new Picture(fPicture.getAbsolutePath());
      }
    }
  }

  protected ScriptCell asScript() {
    if (!value.startsWith("{")) {
      value = "{" + value;
    }
    cellType = CellType.SCRIPT;
    return this;
  }

  protected boolean isScript() {
    return CellType.SCRIPT.equals(cellType);
  }

  protected ScriptCell asVariable() {
    if (!value.startsWith("=")) {
      value = "=" + value;
    }
    cellType = CellType.VARIABLE;
    return this;
  }

  protected boolean isVariable() {
    return CellType.VARIABLE.equals(cellType);
  }

  protected String eval(int row, int col) {
    return "";
  }

  protected boolean isEmpty() {
    return SX.isNotSet(value);
  }

  protected boolean isLineEmpty() {
    for (ScriptCell cell : script.data.get(row)) {
      if (!cell.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  protected boolean isValid() {
    boolean valid = SX.isSet(value);
    if (valid && isImage()) {
      valid &= !value.contains("?");
    }
    return valid;
  }

  protected String get() {
    return value;
  }

  protected ScriptCell set(int row, int col) {
    this.row = row;
    this.col = col;
    return this;
  }

  protected ScriptCell set(String value) {
    this.value = value;
    return this;
  }

  protected List<ScriptCell> setLine(String... items) {
    List<ScriptCell> oldLine = new ArrayList<>();
    for (ScriptCell cell : script.data.get(row)) {
      oldLine.add(cell);
    }
    if (items.length == 0) {
      script.data.set(row, new ArrayList<>());
    } else {
      int col = 1;
      for (String item : items) {
        script.cellAt(row, col++).set(item);
      }
    }
    return oldLine;
  }
}