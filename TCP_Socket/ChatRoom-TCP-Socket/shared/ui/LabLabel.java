package shared.ui;
import javax.swing.*;
public class LabLabel extends JLabel {
 public LabLabel(String text) { super(text); }
 public LabLabel(String text,int align) { super(text,align); }
 @Override public void setText(String text) {
  super.setText(LabIcons.clean(text));
  if (LabIcons.hasSymbol(text)) { setIcon(LabIcons.icon(LabIcons.nameFor(text),16));setIconTextGap(7); }
 }
}
