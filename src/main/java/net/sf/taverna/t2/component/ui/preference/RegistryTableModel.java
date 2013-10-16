/**
 * 
 */
package net.sf.taverna.t2.component.ui.preference;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.table.DefaultTableModel;

import net.sf.taverna.t2.component.api.Registry;

/**
 * @author alanrw
 * 
 */
public class RegistryTableModel extends DefaultTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7789666945764974370L;
	private SortedMap<String, Registry> registryMap = new TreeMap<String, Registry>();

	public RegistryTableModel() {
		super(new String[] { "Registry name", "Registry location" }, 0);
	}

	public void setRegistryMap(SortedMap<String, Registry> registries) {
		registryMap.clear();
		registryMap.putAll(registries);
		updateRows();
	}

	public void updateRows() {
		super.setRowCount(0);
		for (Entry<String, Registry> entry : registryMap.entrySet()) {
			super.addRow(new Object[] { entry.getKey(),
					entry.getValue().getRegistryBase().toExternalForm() });
		}
	}

	public boolean isCellEditable(int row, int column) {
		return false;
	}

	public void removeRow(int row) {
		String key = (String) super.getValueAt(row, 0);
		registryMap.remove(key);
		super.removeRow(row);
	}

	public void insertRegistry(String name, Registry newRegistry) {
		registryMap.put(name, newRegistry);
		updateRows();
	}

	/**
	 * @return the registryMap
	 */
	public SortedMap<String, Registry> getRegistryMap() {
		return registryMap;
	}

}
