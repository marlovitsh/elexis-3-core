/*******************************************************************************
 * Copyright (c) 2005-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.core.ui.contacts.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.core.ui.actions.GlobalActions;
import ch.elexis.core.ui.util.ViewMenus;
import ch.elexis.data.Kontakt;

public class KontaktDetailView extends ViewPart implements ISaveablePart2 {
	public static final String ID = "ch.elexis.KontaktDetailView"; //$NON-NLS-1$
	KontaktBlatt kb;
	
	// +++++ START
	public static Action actionShowEditPart;
	boolean selected = false;
	// +++++ END
	
	public KontaktDetailView(){
		
	}
	
	@Override
	public void createPartControl(Composite parent){
		parent.setLayout(new FillLayout());
		kb = new KontaktBlatt(parent, SWT.NONE, getViewSite());
		ViewMenus menu = new ViewMenus(getViewSite());
		// +++++ START
		//		menu.getContextMenu().add(GlobalActions.printKontaktEtikette);
		actionShowEditPart = new Action("Suchmaske") {
			{
				setToolTipText("Suchmaske");
				ImageDescriptor imageDescriptor = ImageDescriptor.createFromFile(null,
					"/home/empfang/elexis_3_4/ungrad-3-marlovits/ch.marlovits.testingView/rsc/system-search-3.png");
				setImageDescriptor(imageDescriptor);
				//setChecked(false);
			}
			
			@Override
			public void run(){
				selected = !selected;
				// +++++ STARTstart 
				MarlovitsKontaktBlattExtension.setEditing(selected);
				MarlovitsKontaktBlattExtension.setEditing(!MarlovitsKontaktBlattExtension.getEditing());
				MarlovitsKontaktBlattExtension.isSearching = selected;
				// +++++ END
				Kontakt dummy = Kontakt.load(MarlovitsKontaktBlattExtension.dummyPatientID);
				kb.setKontakt(dummy);
				kb.visible(true);
				//setChecked(selected);
			}
		};
		// +++++ END
		menu.createMenu(GlobalActions.printKontaktEtikette
		// +++++ START
			, actionShowEditPart
		// +++++ START
		);
		menu.createToolbar(GlobalActions.printKontaktEtikette
		// +++++ START
			, actionShowEditPart
		// +++++ START
		);
		
	}
	
	@Override
	public void setFocus(){
		kb.setFocus();
	}
	
	/* ******
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2 Wir benötigen das
	 * Interface nur, um das Schliessen einer View zu verhindern, wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */
	public int promptToSaveOnClose(){
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL
				: ISaveablePart2.NO;
	}
	
	public void doSave(IProgressMonitor monitor){ /* leer */}
	
	public void doSaveAs(){ /* leer */}
	
	public boolean isDirty(){
		return true;
	}
	
	public boolean isSaveAsAllowed(){
		return false;
	}
	
	public boolean isSaveOnCloseNeeded(){
		return true;
	}
}
