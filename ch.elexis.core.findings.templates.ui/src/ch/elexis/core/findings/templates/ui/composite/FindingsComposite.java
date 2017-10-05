package ch.elexis.core.findings.templates.ui.composite;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;

import ch.elexis.core.findings.templates.model.FindingsTemplate;
import ch.elexis.core.findings.templates.model.FindingsTemplates;
import ch.elexis.core.findings.templates.model.InputDataGroupComponent;
import ch.elexis.core.findings.templates.ui.util.FindingsTemplateUtil;

public class FindingsComposite extends Composite {
	
	private FindingsDetailComposite findingsDetailComposite;
	private TreeViewer viewer;
	private FindingsTemplates model;
	
	public FindingsComposite(Composite parent, FindingsTemplates model){
		super(parent, SWT.NONE);
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		this.model = model;
	}
	
	public void createContents(){
		viewer = new TreeViewer(this, SWT.FULL_SELECTION | SWT.BORDER);
		viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		ComposedAdapterFactory composedAdapterFactory =
			new ComposedAdapterFactory(ComposedAdapterFactory.Descriptor.Registry.INSTANCE);
		
		viewer.setContentProvider(new FindingsTemplateContentProvider(composedAdapterFactory));
		viewer.setLabelProvider(new FindingsTemplateLabelProvider(composedAdapterFactory));
		
		Resource r = new ResourceImpl();
		r.getContents().add(model);
		createContextMenu(viewer);
		
		viewer.setInput(r);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event){
				Optional<FindingsTemplates> model = getModel();
				if (model.isPresent() && event.getSelection() instanceof StructuredSelection) {
					StructuredSelection s = (StructuredSelection) event.getSelection();
					Object element = s.getFirstElement();
					findingsDetailComposite.setSelection(model.get(),
						element instanceof FindingsTemplate ? (FindingsTemplate) s.getFirstElement()
								: null);
				}
			}
		});
	}
	
	public TreeViewer getViewer(){
		return viewer;
	}
	
	public void setFindingsDetailComposite(FindingsDetailComposite findingsDetailComposite){
		this.findingsDetailComposite = findingsDetailComposite;
	}
	
	public Optional<FindingsTemplates> getModel(){
		Resource r = (Resource) viewer.getInput();
		if (r != null && !r.getContents().isEmpty()) {
			return Optional.of((FindingsTemplates) r.getContents().get(0));
		}
		return Optional.empty();
	}
	
	public void setModel(FindingsTemplates model){
		Resource r = new ResourceImpl();
		r.getContents().add(model);
		viewer.setInput(r);
		if (!model.getFindingsTemplates().isEmpty()) {
			viewer.setSelection(new StructuredSelection(model.getFindingsTemplates().get(0)));
		}
	}
	
	private void createContextMenu(Viewer viewer){
		MenuManager contextMenu = new MenuManager();
		contextMenu.setRemoveAllWhenShown(true);
		contextMenu.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager mgr){
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				if (selection.getFirstElement() instanceof FindingsTemplate) {
					fillContextMenu(mgr, (FindingsTemplate) selection.getFirstElement());
				}
			}
		});
		
		Menu menu = contextMenu.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
	}
	
	private void fillContextMenu(IMenuManager contextMenu, FindingsTemplate findingsTemplate){
		contextMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		contextMenu.add(new Action("entfernen") {
			@Override
			public void run(){
				if (getModel().isPresent())
				{
					if (findingsTemplate.eContainer() instanceof FindingsTemplates) {
						((FindingsTemplates) findingsTemplate.eContainer()).getFindingsTemplates()
							.remove(findingsTemplate);
					}
					else if (findingsTemplate.eContainer() instanceof InputDataGroupComponent) {
						((InputDataGroupComponent) findingsTemplate.eContainer())
							.getFindingsTemplates().remove(findingsTemplate);
						
					}
					getViewer().refresh();
				}
			}
		});
	}
	
	class FindingsTemplateLabelProvider extends AdapterFactoryLabelProvider {
		
		public FindingsTemplateLabelProvider(AdapterFactory adapterFactory){
			super(adapterFactory);
		}
		
		@Override
		public Image getImage(Object object){
			Image img = FindingsTemplateUtil.getImage(object);
			return img != null ? img : super.getImage(object);
		}
	}
	
	class FindingsTemplateContentProvider extends AdapterFactoryContentProvider {
		
		public FindingsTemplateContentProvider(AdapterFactory adapterFactory){
			super(adapterFactory);
		}
		
		@Override
		public boolean hasChildren(Object object){
			if (object instanceof FindingsTemplates) {
				return super.hasChildren(object);
			}
			else if (object instanceof FindingsTemplate) {
				FindingsTemplate findingsTemplate = (FindingsTemplate) object;
				if (findingsTemplate.getInputData() instanceof InputDataGroupComponent) {
					InputDataGroupComponent inputDataGroupComponent =
						(InputDataGroupComponent) findingsTemplate.getInputData();
					return !inputDataGroupComponent.getFindingsTemplates().isEmpty();
				}
			}
			return false;
		}
		
		@Override
		public Object[] getChildren(Object object){
			if (object instanceof FindingsTemplates) {
				return super.getChildren(object);
			} else if (object instanceof FindingsTemplate) {
				FindingsTemplate findingsTemplate = (FindingsTemplate) object;
				if (findingsTemplate.getInputData() instanceof InputDataGroupComponent) {
					InputDataGroupComponent inputDataGroupComponent =
						(InputDataGroupComponent) findingsTemplate.getInputData();
					return inputDataGroupComponent.getFindingsTemplates().toArray();
				}
			}
			return Collections.EMPTY_LIST.toArray();
		}
		
	}
}