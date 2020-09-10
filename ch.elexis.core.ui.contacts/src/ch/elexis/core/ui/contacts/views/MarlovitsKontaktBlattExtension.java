package ch.elexis.core.ui.contacts.views;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.events.ElexisEvent;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.data.events.ElexisEventListener;
import ch.elexis.core.ui.events.ElexisUiEventListenerImpl;
import ch.elexis.core.ui.locks.ToggleCurrentKontaktLockHandler;
import ch.elexis.core.ui.util.LabeledInputField;
import ch.elexis.core.ui.util.LabeledInputField.AutoForm;
import ch.elexis.core.ui.util.LabeledInputField.InputData;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.rgw.tools.JdbcLink;

public class MarlovitsKontaktBlattExtension {
	// +++++ must initialize
	static KontaktBlatt kb = null;
	static AutoForm afDetails;
	static Composite myself = null;
	static IViewSite vs = null;
	
	public static void init(KontaktBlatt pkb, AutoForm pafDetails, Composite pmyself,
		IViewSite pvs){
		kb = pkb;
		afDetails = pafDetails;
		myself = pmyself;
		vs = pvs;
	}
	
	// *** Settings
	static String dummyPatientID = "Q494af38dccda950f026";
	static Color highlightColor = new Color(null, 255, 220, 220);
	static String[] columnDbNames = {
		"Bezeichnung1", "Bezeichnung2", "Geburtsdatum", "Strasse", "Plz", "Ort", "Telefon1",
		"NatelNr", "Telefon2", "Fax", Kontakt.FLD_E_MAIL
	};
	static String[] columnLabels = {
		"Name", "Vorname", "geb.", "Strasse", "PLZ", "Ort", "Festnetz", "Mobil", "Arbeit", "Fax",
		"Email"
	};
	static int colWidthPhone = 150;
	static int colWidthNames = 100;
	static int[] columnWidths = {
		colWidthNames, colWidthNames, 90, 150, 55, 100, colWidthPhone, colWidthPhone, colWidthPhone,
		colWidthPhone, colWidthPhone
	};
	
	static int maxRecordsShown = 100;
	
	static Color savedColor = null;
	static boolean isSearching = false;
	static Composite searchPart;
	static TableViewer tv;
	
	static KeyListener keyListener;
	
	static LinkedList<TableColumn> tableColumns = new LinkedList<TableColumn>();
	
	public static boolean getEditing(){
		return isSearching;
	}
	
	public static void setEditing(boolean editing){
		isSearching = editing;
	}
	
	public static void addSearchSelectionListeners(){
		Control[] children = afDetails.getChildren();
		for (Control ccc : children) {
			LabeledInputField lif = (LabeledInputField) ccc;
			if (!lif.getVisible()) {
				continue;
			}
			Control ctl = lif.getControl();
			ctl.addKeyListener(keyListener);
		}
	}
	
	public static void removeSearchSelectionListeners(){
		if (keyListener == null) {
			return;
		}
		Control[] children = afDetails.getChildren();
		for (Control ccc : children) {
			LabeledInputField lif = (LabeledInputField) ccc;
			if (!lif.getVisible()) {
				continue;
			}
			Control ctl = lif.getControl();
			ctl.removeKeyListener(keyListener);
		}
	}
	
	public static void removeSearchPart(Composite parent){
		if (searchPart == null) {
			return;
		}
		if (!searchPart.isDisposed()) {
			searchPart.dispose();
		}
		removeSearchSelectionListeners();
		setFieldColors();
		parent.getParent().layout(true, true);
	}
	
	/** OK
	 * If searching then rename Telefon1 label to "All Numbers" / disable the other phone number
	 * labels.
	 */
	public static void modifyPhoneFieldLabels(){
		Control[] children = afDetails.getChildren();
		for (Control ccc : children) {
			// *** get db field name
			LabeledInputField lif = (LabeledInputField) ccc;
			Control ctl = lif.getControl();
			InputData inputData = (InputData) ctl.getData();
			Field field;
			String fieldName = "";
			try {
				field = inputData.getClass().getDeclaredField("sFeldname");
				field.setAccessible(true);
				fieldName = (String) field.get(inputData);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
					| IllegalAccessException e) {
				e.printStackTrace();
			}
			switch (fieldName.toLowerCase()) {
			case "telefon1":
				Label lbl = lif.getLabelComponent();
				String strLabel = (String) lbl.getData("savedLabel");
				if (strLabel == null || strLabel.isEmpty()) {
					lbl.setData("savedLabel", lif.getLabel());
				}
				if (isSearching) {
					lbl.setText("Festnetz/Gesch./Mobil");
				} else {
					lbl.setText((String) lbl.getData("savedLabel"));
				}
				break;
			case "telefon2":
			case "natelnr":
				ctl.setEnabled(!isSearching);
				break;
			case "fax":
				break;
			}
		}
	}
	
	/**
	 * OK create an item for the table header column-selection-menu
	 * 
	 * @param parent:
	 *            parent Mnue
	 * @param column:
	 *            the table column for which to create a menu item
	 */
	static void createMenuItem(Menu parent, final TableColumn column){
		final MenuItem itemName = new MenuItem(parent, SWT.CHECK);
		itemName.setText(column.getText());
		itemName.setSelection(column.getResizable());
		itemName.addListener(SWT.Selection, event -> {
			if (itemName.getSelection()) {
				column.setWidth(150);
				column.setResizable(true);
			} else {
				column.setWidth(0);
				column.setResizable(false);
			}
		});
	}
	
	/**
	 * OK set the color of search fields with content to some highlight color
	 */
	public static void setFieldColors(){
		Control[] children = afDetails.getChildren();
		for (Control ccc : children) {
			// *** skip visible
			LabeledInputField lif = (LabeledInputField) ccc;
			if (!lif.getVisible()) {
				continue;
			}
			// *** save bg color for restoring later
			Control ctl = lif.getControl();
			if (afDetails.getData("savedCtlBackgroundColor") == null) {
				afDetails.setData("savedCtlBackgroundColor", ctl.getBackground());
			}
			
			// *** saved bg color if not searching
			if (!isSearching) {
				ctl.setBackground((Color) afDetails.getData("savedCtlBackgroundColor"));
			}
			
			InputData inputData = (InputData) ctl.getData();
			// *** skip UUID
			Field field;
			try {
				field = inputData.getClass().getDeclaredField("sFeldname");
				field.setAccessible(true);
				String fieldName = (String) field.get(inputData);
				if (fieldName.equalsIgnoreCase("UUID")) {
					continue;
				}
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
					| IllegalAccessException e) {
				e.printStackTrace();
			}
			// *** get field text, set bg to highlight color if not empty/not null
			String text = inputData.getText();
			if (text == null || text.isEmpty()) {
				ctl.setBackground((Color) afDetails.getData("savedCtlBackgroundColor"));
			} else {
				ctl.setBackground(highlightColor);
			}
		}
	}
	
	public static void addSearchPart(Composite parent){
		// *** set bg color for fields with contents
		if (keyListener == null) {
			keyListener = new KeyListener() {
				
				@Override
				public void keyPressed(KeyEvent arg0){}
				
				@Override
				public void keyReleased(KeyEvent arg0){
					setFieldColors();
					tv.refresh();
				}
			};
		}
		
		searchPart = new Composite(parent, SWT.NONE);
		searchPart.addListener(0xFFFFFFFF, new Listener() {
			@Override
			public void handleEvent(Event arg0){
				System.out.println(arg0.toString());
			}
		});
		//searchPart.setVisible(false);
		//		GridData gridData = SWTHelper.getFillGridData(1, true, 1, false);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		//gridData.widthHint = 200;
		gridData.heightHint = 200;
		gridData.horizontalSpan = 20;
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		searchPart.setLayout(gridLayout);
		searchPart.setLayoutData(gridData);
		
		// *********************************************************
		final Menu headerMenu = new Menu(myself.getShell(), SWT.POP_UP);
		
		tv = new TableViewer(searchPart);
		
		// Set the content and label providers
		tv.setContentProvider(new SearchContentProvider());
		tv.setLabelProvider(new MarlovitsSearchLabelProvider());
		tv.setSorter(new MarlovitsSearchViewerSorter());
		tv.getTable().setBackground(highlightColor);
		
		// Set up the table
		Table table = tv.getTable();
		table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL + GridData.FILL_VERTICAL));
		
		tableColumns.clear();
		for (int i = 0; i < columnLabels.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.LEFT);
			tableColumns.add(tc);
			tc.setText(columnLabels[i]);
			tc.setMoveable(true);
			tc.setData("fieldDbName", columnDbNames[i]);
			tc.setData("colNum", i);
			table.getColumn(i).pack();
			int width = columnWidths[i];
			if (width > 0) {
				tc.setWidth(width);
			}
			final ViewerComparator theComparator = tv.getComparator();
			final int ii = i;
			tc.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event){
					((MarlovitsSearchViewerSorter) tv.getSorter()).doSort(ii);
					tv.refresh();
				}
			});
			createMenuItem(headerMenu, tc);
		}
		
		final Menu tableMenu = new Menu(myself.getShell(), SWT.POP_UP);
		MenuItem item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("DUMMY");
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Open");
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Open With");
		new MenuItem(tableMenu, SWT.SEPARATOR);
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Cut");
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Copy");
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Paste");
		new MenuItem(tableMenu, SWT.SEPARATOR);
		item = new MenuItem(tableMenu, SWT.PUSH);
		item.setText("Delete");
		
		tv.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event){
				isSearching = false;
				kb.visible(true);
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				Kontakt kontakt = (Kontakt) sel.getFirstElement();
				ElexisEventDispatcher.fireSelectionEvent(kontakt);
				if (kontakt.istPatient()) {
					Patient patient = Patient.load(kontakt.getId());
					ElexisEventDispatcher.fireSelectionEvent(patient);
					try {
						PatientDetailView2 pdv =
							(PatientDetailView2) vs.getPage().showView(PatientDetailView2.ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		// Turn on the header and the lines
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		tv.refresh();
		LinkedList<String> entries = new LinkedList<String>();
		entries.add("first");
		entries.add("2nd");
		entries.add("3rd");
		entries.add("4th");
		entries.add("4th");
		entries.add("4th");
		entries.add("4th");
		entries.add("4th");
		entries.add("4th");
		tv.setInput(entries);
		
		table.addListener(SWT.MenuDetect, event -> {
			Point pt = myself.getDisplay().map(null, table, new Point(event.x, event.y));
			Rectangle clientArea = table.getClientArea();
			boolean header =
				clientArea.y <= pt.y && pt.y < (clientArea.y + table.getHeaderHeight());
			table.setMenu(header ? headerMenu : tableMenu);
		});
		
		/* IMPORTANT: Dispose the menus (only the current menu, set with setMenu(), will be automatically disposed) */
		table.addListener(SWT.Dispose, event -> {
			headerMenu.dispose();
			tableMenu.dispose();
		});
		
		setFieldColors();
		
		//		TableColumn firstCol = tableColumns.get(0);
		//		tv.getTable().setSortColumn(firstCol);
		//		tv.getTable().setSortDirection(SWT.UP);
		
		addSearchSelectionListeners();
		setFieldColors();

		parent.layout(true, true);
		
		//addSearchSelectionListeners();
	}
	
	public static String normalizePhoneNumber(String phoneNumber){
		// +++++
		phoneNumber = phoneNumber.replaceAll("\\s", "");
		// *** convert any CH number from 0041 xx yy zz to 0xx yy zz
		phoneNumber = phoneNumber.replaceFirst("^00410", "0");
		phoneNumber = phoneNumber.replaceFirst("^0041", "0");
		phoneNumber = phoneNumber.replaceFirst("^\\+410", "0");
		phoneNumber = phoneNumber.replaceFirst("^\\+41", "0");
		// *** convert D numbers all to the same
		phoneNumber = phoneNumber.replaceFirst("^00490", "0049");
		phoneNumber = phoneNumber.replaceFirst("^\\+490", "0049");
		phoneNumber = phoneNumber.replaceFirst("^\\+49", "0049");
		//		phoneNumber = phoneNumber.replaceFirst("^0", "");
		String tmp = phoneNumber.replaceAll("[^0-9%-_()\\|\\]\\[]", "");
		return tmp;
	}
	
	/**
	 * This class provides the content for the table
	 */
	public static class SearchContentProvider implements IStructuredContentProvider {
		
		/**
		 * Gets the elements for the table
		 * 
		 * @param arg0
		 *            the model
		 * @return Object[]
		 */
		public Object[] getElements(Object arg0){
			// *** build query on Kontakt using the values in the form - loop through afDetails
			Query<Kontakt> kontakte = new Query<Kontakt>(Kontakt.class);
			// *** use dummy patient's values
			kontakte.add(Kontakt.FLD_ID, Query.NOT_EQUAL, dummyPatientID);
			// *** no mandator, no user
			kontakte.add(Kontakt.FLD_IS_MANDATOR, Query.NOT_EQUAL, "true");
			kontakte.add(Kontakt.FLD_IS_USER, Query.NOT_EQUAL, "true");
			// *** loop through afDetails fields
			Control[] children = afDetails.getChildren();
			for (Control ccc : children) {
				// *** skip invisibles
				LabeledInputField lif = (LabeledInputField) ccc;
				if (!lif.getVisible()) {
					continue;
				}
				Control ctl = lif.getControl();
				InputData inputData = (InputData) ctl.getData();
				Field field;
				String fieldName = "";
				boolean isPhone = false;
				String text = "";
				try {
					field = inputData.getClass().getDeclaredField("sFeldname");
					field.setAccessible(true);
					fieldName = (String) field.get(inputData);
					// *** skip UUID
					if (fieldName.equalsIgnoreCase("UUID")) {
						continue;
					}
					// *** skip empty fields
					text = inputData.getText();
					if ((text == null) || (text.isEmpty())) {
						continue;
					}
					if (fieldName.equalsIgnoreCase("telefon1")) {
						kontakte.and();
						kontakte.startGroup();
						String normalizedPhoneNumber = normalizePhoneNumber(text);
						System.out.println("normalizedPhoneNumber: " + normalizedPhoneNumber);
						//regexp_replace(Natelnr, '[^0-9]', '')
						// +++++ only postgresql -> RLIKE for mysql
						kontakte.addToken(
							"regexp_replace(" + Kontakt.FLD_PHONE1 + ", '[^0-9]', '', 'g') "
								+ " SIMILAR TO " + "'" + normalizedPhoneNumber + "%'");
						kontakte.or();
						kontakte.addToken(
							"regexp_replace(" + Kontakt.FLD_PHONE2 + ", '[^0-9]', '', 'g') "
								+ " SIMILAR TO " + "'" + normalizedPhoneNumber + "%'");
						kontakte.or();
						kontakte.addToken(
							"regexp_replace(" + Kontakt.FLD_MOBILEPHONE + ", '[^0-9]', '', 'g') "
								+ " SIMILAR TO " + "'" + normalizedPhoneNumber + "%'");
						kontakte.and();
						kontakte.endGroup();
					}
					if (fieldName.equalsIgnoreCase("telefon1")) {
						continue;
					}
					if (fieldName.equalsIgnoreCase("telefon2")) {
						continue;
					}
					if (fieldName.equalsIgnoreCase("natelnr")) {
						continue;
					}
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
						| IllegalAccessException e) {
					e.printStackTrace();
				}
				text = inputData.getText();
				if (!isPhone)
					kontakte.add(fieldName, Query.LIKE, text + "%", true);
			}
			
			// *** do not return more than 100 items
			JdbcLink j1 = PersistentObject.getConnection();
			String dbFlavor = j1.DBFlavor;
			
			// *** build sql clause with limits/offsets parts
			String casesSql = "";
			int offsetCases = 0;
			if (dbFlavor.equalsIgnoreCase("postgresql")) {
				casesSql = casesSql + " offset " + offsetCases + " limit " + maxRecordsShown;
			} else if (dbFlavor.equalsIgnoreCase("mysql")) {
				casesSql = casesSql + " limit " + offsetCases + ", " + maxRecordsShown;
			} else if (dbFlavor.equalsIgnoreCase("h2")) {
				casesSql = casesSql + " offset " + offsetCases + " limit " + maxRecordsShown;
			}
			kontakte.addToken(" 1=1 " + casesSql);
			
			Object[] result = new Object[0];
			try {
				result = kontakte.execute().toArray();
			} catch (Exception e) {}
			return result;
		}
		
		public void dispose(){}
		
		public void inputChanged(Viewer arg0, Object arg1, Object arg2){}
	}
	
	/**
	 * OK get the labels/images for the table
	 */
	public static class MarlovitsSearchLabelProvider implements ITableLabelProvider {
		public Image getColumnImage(Object arg0, int arg1){
			return null;
		}
		
		/**
		 * get the label for the column
		 */
		public String getColumnText(Object arg0, int arg1){
			Kontakt kontakt = (Kontakt) arg0;
			return kontakt.get(columnDbNames[arg1]);
		}
		
		@Override
		public void addListener(ILabelProviderListener arg0){}
		
		@Override
		public void dispose(){}
		
		@Override
		public boolean isLabelProperty(Object arg0, String arg1){
			return false;
		}
		
		@Override
		public void removeListener(ILabelProviderListener arg0){}
	}
	
	/**
	 * OK This class implements the sorting for the search results table
	 */
	public static class MarlovitsSearchViewerSorter extends ViewerSorter {
		/**
		 * OK Sort the table. If no sort column is specified yet, then start with ascending. Else
		 * cycle through ascending - descending - noSort.
		 * 
		 * @param column
		 *            the column to be sorted
		 */
		public void doSort(int column){
			Table table = tv.getTable();
			TableColumn curColumn = table.getSortColumn();
			TableColumn newColumn = tableColumns.get(column);
			if (curColumn == newColumn) {
				switch (table.getSortDirection()) {
				case SWT.UP:
					table.setSortDirection(SWT.DOWN);
					break;
				case SWT.DOWN:
					table.setSortDirection(SWT.NONE);
					table.setSortColumn(null);
					break;
				case SWT.NONE:
					table.setSortDirection(SWT.UP);
					break;
				}
			} else {
				// *** by default start with ascending
				table.setSortDirection(SWT.UP);
				table.setSortColumn(newColumn);
			}
		}
		
		/**
		 * OK compare two values for the sorting of a table
		 * 
		 * @param1 viewer - the viewer to be sorted
		 * @param2 object1 - object1 (Kontakt) to be compared
		 * @param2 object2 - object2 (Kontakt) to be compared
		 */
		public int compare(Viewer viewer, Object object1, Object object2){
			Table table = tv.getTable();
			TableColumn tc = table.getSortColumn();
			if (tc == null) {
				return 0;
			}
			if (table.getSortDirection() == SWT.NONE) {
				return 0;
			}
			String fieldDbName = (String) tc.getData("fieldDbName");
			int curColNum = (int) tc.getData("colNum");
			
			// *** get the strings to be compared
			String compareStr1 = ((Kontakt) object1).get(fieldDbName).toLowerCase();
			String compareStr2 = ((Kontakt) object2).get(fieldDbName).toLowerCase();
			if (compareStr1 == null) {
				compareStr1 = "";
			}
			if (compareStr2 == null) {
				compareStr2 = "";
			}
			
			// *** sort according to col type
			int result = 0;
			switch (curColNum) {
			case 0:
			case 1:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
				// *** use normalizer for correct sorting of umlauts
				compareStr1 =
					java.text.Normalizer.normalize(compareStr1, java.text.Normalizer.Form.NFD);
				compareStr2 =
					java.text.Normalizer.normalize(compareStr2, java.text.Normalizer.Form.NFD);
				result = compareStr1.compareTo(compareStr2);
				break;
			case 2:
				DateFormat format = new SimpleDateFormat("dd.MM.yyyy");
				Date date1 = null;
				Date date2 = null;
				try {
					date1 = format.parse(compareStr1);
				} catch (ParseException e) {}
				try {
					date2 = format.parse(compareStr2);
				} catch (ParseException e) {}
				if (date1 == null && date2 == null) {
					return 0;
				} else if (date1 == null) {
					return (table.getSortDirection() == SWT.UP) ? -1 : 1;
				} else if (date2 == null) {
					return (table.getSortDirection() == SWT.UP) ? 1 : -1;
				}
				result = date1.compareTo(date2);
				break;
			}
			
			// *** descending -> switch direction
			if (table.getSortDirection() == SWT.DOWN)
				result = -result;
			
			return result;
		}
	}
	

}

// *********************************************************

//		btnDoSearch.addSelectionListener(new SelectionListener() {
//			@Override
//			public void widgetDefaultSelected(SelectionEvent arg0){}
//			
//			@Override
//			public void widgetSelected(SelectionEvent arg0){
//				String sql = "select * from kontakt where deleted = '0' ";
//				Control[] children = afDetails.getChildren();
//				for (Control ccc : children) {
//					LabeledInputField lif = (LabeledInputField) ccc;
//					if (!lif.getVisible()) {
//						continue;
//					}
//					Control ctl = lif.getControl();
//					InputData inputData = (InputData) ctl.getData();
//					Field field;
//					String fieldName = "";
//					try {
//						field = inputData.getClass().getDeclaredField("sFeldname");
//						field.setAccessible(true);
//						fieldName = (String) field.get(inputData);
//						if (fieldName.equalsIgnoreCase("UUID")) {
//							continue;
//						}
//					} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
//							| IllegalAccessException e) {
//						e.printStackTrace();
//					}
//					String text = inputData.getText();
//					if (!text.isEmpty()) {
//						sql = sql + " and " + fieldName + " ilike '" + text + "%' ";
//						CommonViewer commonViewer;
//						ControlFieldListener cfl;
//					}
//				}
//				System.out.println(sql);
//			}
//		});

////	try {
////	KontakteView kontakteView =
////		(KontakteView) vs.getPage().showView(KontakteView.ID);
////	Field field = kontakteView.getClass().getDeclaredField("loader");
////	field.setAccessible(true);
////	FlatDataLoader flatDataLoader = (FlatDataLoader) field.get(kontakteView);
////	
////	Field field2 =
////		flatDataLoader.getClass().getSuperclass().getDeclaredField("queryFilters");
////	field2.setAccessible(true);
////	LinkedList<QueryFilter> queryFilters =
////		(LinkedList<QueryFilter>) field2.get(flatDataLoader);
////	
////	Field field3 =
////		flatDataLoader.getClass().getSuperclass().getDeclaredField("qbe");
////	field3.setAccessible(true);
////	Query<? extends PersistentObject> qbe =
////		(Query<? extends PersistentObject>) field3.get(flatDataLoader);
////	
////	qbe.clear();
////	qbe.add(Kontakt.FLD_NAME2, Query.LIKE, "Hara", true);
////	//qbe.execute();
////	//					kontakteView.notify();
////	//					kontakteView.selected();
////	HashMap<String, String> params = new HashMap<String, String>();
////	params.put(Kontakt.FLD_NAME2, "Harald");
////	//					kontakteView.changed(params);
////	
////	final ProgressMonitorDialog dialog = new ProgressMonitorDialog(site.getShell());
////	//					dialog.open();
////	//					dialog.setCancelable(true);
////	final IProgressMonitor progressMonitor = dialog.getProgressMonitor();
////	
////	HashMap<String, Object> params2 = new HashMap<String, Object>();
////	params2.put(Kontakt.FLD_NAME2, "Harald");
////	HashMap<String, Object> values = new HashMap<String, Object>();
////	values.put("Bezeichnung2", "Harald");
////	flatDataLoader.work(progressMonitor, params2);
////	//					flatDataLoader.changed(params);
////	
////	//					KontaktSelector kontaktSelector;
////	//					KontaktFilter fp = new KontaktFilter(0);
////	//					queryFilters.add(e)
////	kontakteView.reorder(Kontakt.FLD_NAME2);
////	
////	//flatDataLoader.changed(values);
////	//					java.util.List<PersistentObject> res = new java.util.LinkedList<PersistentObject>();
////	//res.add(e)
////	//					flatDataLoader.setResult(res);
////	//					flatDataLoader.applyQueryFilters();
////	//					flatDataLoader.updateElement(0);
////	//					IProgressMonitor monitor = null;
////	//					HashMap<String, Object> params = new HashMap<String, Object>();
////	//					params.put("Bezeichnung2", (Object)"Harald");
////	//					flatDataLoader.work(monitor, params);
////} catch (PartInitException | NoSuchFieldException | SecurityException
////		| IllegalArgumentException | IllegalAccessException e) {
////	e.printStackTrace();
////}
