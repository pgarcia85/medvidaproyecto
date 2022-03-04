package com.medvida.webscript;

import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.servlet.FormData;

import com.medvida.error.MedvidaException;
import com.medvida.model.MedvidaModel;
import com.medvida.utils.Constantes;

public class CrearDocumentoWebScript extends DeclarativeWebScript {

	private static final Logger LOGGER = LoggerFactory.getLogger(CrearDocumentoWebScript.class);

	private ServiceRegistry serviceRegistry;

	private SearchService searchService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		long idOperacion = System.currentTimeMillis();
		Date startDate = new Date();
		LOGGER.info(
				"INICIO--> CrearDocumentoWebScript.execute " + startDate + " id-operación: " + idOperacion + " <-----");
		System.out.println("");
		System.out.println(
				"INICIO--> CrearDocumentoWebScript.execute " + startDate + " id-operación: " + idOperacion + " <-----");
		Map<String, Object> parametrosServicio = new HashMap<String, Object>();
		Map<String, Object> model = new HashMap<String, Object>();
		String mimetype = null;
		InputStream content = null;
		String documentName = null;
		FormData form = (FormData) req.parseContent();
		NodeRef archivoNuevo = null;
		try {
			// Se obtienen los datos enviados en el formulario
			for (FormData.FormField field : form.getFields()) {
				if (!field.getIsFile()) {
					parametrosServicio.put(field.getName(), field.getValue());
					LOGGER.info("id-operación: " + idOperacion + ". Parametro de entrada -> " + field.getName() + " "
							+ field.getValue());
					System.out.println("id-operación: " + idOperacion + ". Parametro de entrada -> " + field.getName()
							+ " ,valor:" + field.getValue());
				} else {
					mimetype = field.getMimetype();
					content = field.getInputStream();
					documentName = field.getFilename();
					LOGGER.info("id-operación: " + idOperacion + ". Contenido documento, Mimetype: " + mimetype
							+ " Nombre: " + documentName);
					System.out.println("id-operación: " + idOperacion + ". Contenido documento, Mimetype: " + mimetype
							+ " Nombre: " + documentName);
				}
			}

			// Verificar que tenemos los datos básicos
			String identificadorDoc = null;
			String contract_id = (String) parametrosServicio.get("contract_id");
			String person_id = (String) parametrosServicio.get("person_id");
			String typology = (String) parametrosServicio.get("typology");
			String type = (String) parametrosServicio.get("type");
			if (contract_id.isBlank() && person_id.isBlank() && type.isBlank()) {
				// error que no hay identificador
				model.put("error",
						"No se dispone de: identificador de contrato, identificador de persona, tipo documento (Person, Contract)");
				return model;
			} else if (!contract_id.isBlank()) {
				identificadorDoc = contract_id;
			} else if (!person_id.isBlank()) {
				identificadorDoc = person_id;
			}
		
			// Obtener la carpeta origen donde guardar el documento
			NodeRef carpetaPadre = getPadre(contract_id, person_id, type, idOperacion);

			// Crear la ruta completa donde se guardará el documento
			NodeRef carpetaFinal = crearCarpetas(carpetaPadre, generarRuta(identificadorDoc, typology), idOperacion);

			// Crear el documento y su contenido
			archivoNuevo = crearDocumento(parametrosServicio, mimetype, content, documentName, carpetaFinal,
					idOperacion);
			model.put("idNodo", archivoNuevo.getId());
		} catch (Exception e) {
			model.put("error", "Se ha producido un error al crear el documento: " + e.getMessage());
			return model;
		}
		LOGGER.info("id-operación: " + idOperacion + ". Parametro de salida-> uuid documento nuevo: "
				+ archivoNuevo.getId());
		LOGGER.info(
				"FIN--> CrearDocumentoWebScript.execute " + startDate + " ,id-operación: " + idOperacion + " <-----");
		
		System.out.println("id-operación: " + idOperacion + ". Parametro de salida-> uuid documento nuevo: "
				+ archivoNuevo.getId());
		System.out.println(
				"FIN--> CrearDocumentoWebScript.execute " + startDate + " ,id-operación: " + idOperacion + " <-----");
		
		return model;
	}

	/**
	 * Método que crea un documento con su contenido
	 * 
	 * @param parametrosServicio Mapa con los parametros que obtnemos del formulario
	 * @param mimetype,          tipo de documento creado
	 * @param content,           contenido del documento creado
	 * @param documentName,      nombre del documento creado
	 * @param carpetaFinal,      nodo folder donde se crea el documento
	 * @param idOperacion,       identificador de la peticion
	 * @return Nodo creado
	 * @throws MedvidaException
	 */
	private NodeRef crearDocumento(Map<String, Object> parametrosServicio, String mimetype, InputStream content,
			String documentName, NodeRef carpetaFinal, long idOperacion) throws MedvidaException {
		LOGGER.info("INICIO--> crearDocumento, id-operación: " + idOperacion + " <-----");
		System.out.println("INICIO--> crearDocumento, id-operación: " + idOperacion + " <-----");
		// Crea un archivo bajo la carpeta indicada en folderInfo y esta vacio al inicio
		FileInfo fileInfo = serviceRegistry.getFileFolderService().create(carpetaFinal, documentName,
				MedvidaModel.TYPE_INSURANCE_DOC);
		// Obtiene el nodeRef del nuevo archivo
		NodeRef archivoNuevo = fileInfo.getNodeRef();
		// añadimos properties
		serviceRegistry.getNodeService().addProperties(archivoNuevo,
				obtenerMapaPropiedadesDocumento(parametrosServicio, idOperacion));

		// Añade contenido al archivo

		ContentWriter writer = serviceRegistry.getFileFolderService().getWriter(archivoNuevo);
		writer.setMimetype(mimetype);
		writer.setEncoding("UTF-8");
		writer.putContent(content);

		LOGGER.info("FIN--> crearDocumento, id-operación: " + idOperacion + " <-----");
		System.out.println("FIN--> crearDocumento, id-operación: " + idOperacion + " <-----");
		return archivoNuevo;
	}

	/**
	 * Genera un Map<QName, Serialiable> con las propiedades del documento
	 * 
	 * @param recibe un map con la propiedades del formulario
	 * @return Map<QName, Serialiable> propiedades
	 * @throws MedvidaException
	 */
	private Map<QName, Serializable> obtenerMapaPropiedadesDocumento(Map<String, Object> propiedades, long idOperacion)
			throws MedvidaException {
		LOGGER.info("INICIO--> obtenerMapaPropiedadesDocumento, id-operación: " + idOperacion + " <-----");
		System.out.println("INICIO--> obtenerMapaPropiedadesDocumento, id-operación: " + idOperacion + " <-----");
		Map<QName, Serializable> mapaPropiedades = new HashMap<QName, Serializable>();
		SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
		try {
			String lopd_level = (String) propiedades.get("lopd_level");
			if(StringUtils.isBlank(lopd_level)) {
				lopd_level=null;
			}
			mapaPropiedades.put(MedvidaModel.PROP_NIVEL_LOPD, lopd_level.trim());
			mapaPropiedades.put(MedvidaModel.PROP_CONTRACT_ID, propiedades.get("contract_id").toString().trim());
			mapaPropiedades.put(MedvidaModel.PROP_CONTRAC_ID_BS, propiedades.get("contract_id_BS").toString().trim());
			mapaPropiedades.put(MedvidaModel.PROP_PERSON_ID, propiedades.get("person_id").toString().trim());
			String type = (String) propiedades.get("type");
			if(StringUtils.isBlank(type.trim())) {
				type=null;
			}
			mapaPropiedades.put(MedvidaModel.PROP_TYPE, type.trim());
			mapaPropiedades.put(MedvidaModel.PROP_TYPOLOGY, propiedades.get("typology").toString().trim());
			mapaPropiedades.put(MedvidaModel.PROP_TYPOLOGY_DESCRIPTION, propiedades.get("typology_description").toString().trim());
			String start_date = propiedades.get("start_date").toString().trim();
			String end_date = propiedades.get("end_date").toString().trim();
			String update_date = propiedades.get("update_date").toString().trim();
			if (!StringUtils.isBlank(start_date)) {
				mapaPropiedades.put(MedvidaModel.PROP_START_DATE, formato.parse(start_date));
			}
			if (!StringUtils.isBlank(end_date)) {
				mapaPropiedades.put(MedvidaModel.PROP_END_DATE, formato.parse(end_date));
			}
			if (!StringUtils.isBlank(update_date)) {
				mapaPropiedades.put(MedvidaModel.PROP_UPDATE_DATE, formato.parse(update_date));
			}
			mapaPropiedades.put(MedvidaModel.PROP_COD_CAJA, propiedades.get("cod_caja").toString().trim());
			mapaPropiedades.put(MedvidaModel.PROP_COD_NUM_CAJA, propiedades.get("cod_num_caja").toString().trim());
			String sec_number = propiedades.get("sec_number").toString().trim();
			String page_number = propiedades.get("page_number").toString().trim();
			String lote_number =  propiedades.get("lote_number").toString().trim();
			String fiscal_year = propiedades.get("fiscal_year").toString().trim();
			if (!StringUtils.isBlank(sec_number)) {
				mapaPropiedades.put(MedvidaModel.PROP_SEC_NUMBER, sec_number);
			}
			if (!StringUtils.isBlank(page_number)) {
				mapaPropiedades.put(MedvidaModel.PROP_PAGE_NUMBER, page_number);
			}
			if (!StringUtils.isBlank(lote_number)) {
				mapaPropiedades.put(MedvidaModel.PROP_LOTE_NUMBER, lote_number);
			}
			if (!StringUtils.isBlank(fiscal_year)) {
				mapaPropiedades.put(MedvidaModel.PROP_FISCAL_YEAR, fiscal_year);
			}
			mapaPropiedades.put(MedvidaModel.PROP_SUPPLEMENT, propiedades.get("supplement").toString().trim());
		} catch (ParseException e) {
			LOGGER.error("FIN--> obtenerMapaPropiedadesDocumento, id-operación: " + idOperacion + " " + e.getMessage()
					+ " <-----");
			throw new MedvidaException("Se ha produccido un error en el parseo " + e.getMessage());
		} catch (Exception e) {
			LOGGER.error("FIN--> obtenerMapaPropiedadesDocumento, id-operación: " + idOperacion + " " + e.getMessage()
					+ " <-----");
			throw new MedvidaException("Se ha produccido un error al generar el mapa de propiedades del documento");
		}
		LOGGER.info("FIN--> obtenerMapaPropiedadesDocumento, id-operación: " + idOperacion + " <-----");
		System.out.println("FIN--> obtenerMapaPropiedadesDocumento, id-operación: " + idOperacion + " <-----");
		return mapaPropiedades;
	}

	/**
	 * Metodo que genera la ruta donde se aloja el documento
	 * 
	 * @param id,       identificador del documento
	 * @param typology, tipología del documento
	 * @return List, con los nivel de la ruta de carpetas
	 */
	private List<String> generarRuta(String id, String typology) {
		List<String> lista = new ArrayList<String>();
		if (!StringUtils.isBlank(id)) {
			while (id.length() > 3) {
				String nivel = id.substring(0, 1);
				lista.add(nivel);
				id = id.substring(1, id.length());
			}
			lista.add(id);
		}
		if(typology.isBlank()) {
			lista.add(Constantes.NO_TYPOLOGY_DEFINED);
		}else {
			lista.add(typology);
		}
		return lista;

	}

	/**
	 * Obtiene la carpeta origen donde se guarda el documento en funcion de los
	 * parametros recibidos
	 * 
	 * @param contract_id, id de contrato
	 * @param person_id,   id de persona
	 * @param type,        tipo de documento
	 * @return NodeRef, nodo padre
	 */
	private NodeRef getPadre(String contract_id, String person_id, String type, long idOperacion) {

		LOGGER.info("INICIO --> getPadre, id-operación: " + idOperacion + " <-----");
		System.out.println("INICIO --> getPadre, id-operación: " + idOperacion + " <-----");
		
		String origen = null;
		if (contract_id.isBlank() && person_id.isBlank()) {
			origen = Constantes.FL_NO_ID_DEFINED;
		} else if (!contract_id.isBlank()) {
			origen = Constantes.FL_CONTRACT;
		} else if (!person_id.isBlank()) {
			origen = Constantes.FL_PERSON;
		} else if (type.isBlank()) {
			origen = Constantes.FL_NO_TYPE_DEFINED;
		}
		NodeRef padre = serviceRegistry.getSiteService().getContainer(Constantes.SITE_NAME, origen);
		/*StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
		String query = "TYPE:\"cm:folder\" AND =cm:name:\"" + origen + "\"";
		LOGGER.info("getPadre, id-operación: " + idOperacion + "- Se ejecuta la query" + query + " <-----");
		System.out.println("getPadre, id-operación: " + idOperacion + "- Se ejecuta la query =" + query + " <-----");
		ResultSet rs = serviceRegistry.getSearchService().query(storeRef, SearchService.LANGUAGE_FTS_ALFRESCO, query);
		NodeRef padre = rs.getNodeRef(0);*/
		
		LOGGER.info("FIN --> getPadre, id-operación: " + idOperacion + " <-----");
		System.out.println("FIN --> getPadre, id-operación: " + idOperacion + " <-----");
		return padre;

	}

	/**
	 * Crea la ruta de carpetas donde se guarda el documento
	 * 
	 * @param carpetaPadre, nodo de la carpeta origen de almacenamiento
	 * @param pathElements, lista con todos los elemntos del arbol de carpetas
	 * @return Nodo de la carpeta final
	 */
	public NodeRef crearCarpetas(NodeRef carpetaPadre, List<String> pathElements, long idOperacion) {
		LOGGER.info("INICIO--> crearCarpetas, id-operación: " + idOperacion + " <-----");
		System.out.println("INICIO--> crearCarpetas, id-operación: " + idOperacion + " <-----");
		// Nodo padre actual, en que se va situando
		NodeRef currentParentRef = carpetaPadre;
		// recorrer lista donde cada elemento es un directorio
		for (final String element : pathElements) {
			// busca el nodo
			NodeRef nodeRef = serviceRegistry.getNodeService().getChildByName(currentParentRef,
					ContentModel.ASSOC_CONTAINS, element);

			if (nodeRef == null) {
				FileInfo createdFileInfo = serviceRegistry.getFileFolderService().create(currentParentRef, element,
						ContentModel.TYPE_FOLDER);
				currentParentRef = createdFileInfo.getNodeRef();
			} else {
				// it exists
				currentParentRef = nodeRef;
			}

		}
		LOGGER.info("FIN--> crearCarpetas, id-operación: " + idOperacion + " <-----");
		System.out.println("FIN--> crearCarpetas, id-operación: " + idOperacion + " <-----");
		return currentParentRef;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

}
