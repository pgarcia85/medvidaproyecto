package com.medvida.model;

import org.alfresco.service.namespace.QName;

public class MedvidaModel {

	final public static String MED_MODEL_PREFIX = "mv.model";

	final public static QName TYPE_MEDVIDA_DOC = QName.createQName(MED_MODEL_PREFIX, "medvida_doc");

	final public static QName PROP_NIVEL_LOPD = QName.createQName(MED_MODEL_PREFIX, "lopd_level");

	
	final public static QName TYPE_INSURANCE_DOC = QName.createQName(MED_MODEL_PREFIX, "insurance_doc");
	
	final public static QName PROP_CONTRACT_ID = QName.createQName(MED_MODEL_PREFIX, "contract_id");
	final public static QName PROP_CONTRAC_ID_BS = QName.createQName(MED_MODEL_PREFIX, "contract_id_BS");
	final public static QName PROP_PERSON_ID = QName.createQName(MED_MODEL_PREFIX, "person_id");
	final public static QName PROP_TYPE = QName.createQName(MED_MODEL_PREFIX, "type");
	final public static QName PROP_TYPOLOGY = QName.createQName(MED_MODEL_PREFIX, "typology");
	final public static QName PROP_TYPOLOGY_DESCRIPTION = QName.createQName(MED_MODEL_PREFIX, "typology_description");
	final public static QName PROP_START_DATE = QName.createQName(MED_MODEL_PREFIX, "start_date");
	final public static QName PROP_END_DATE = QName.createQName(MED_MODEL_PREFIX, "end_date");
	final public static QName PROP_UPDATE_DATE = QName.createQName(MED_MODEL_PREFIX, "update_date");
	final public static QName PROP_COD_CAJA = QName.createQName(MED_MODEL_PREFIX, "cod_caja");
	final public static QName PROP_COD_NUM_CAJA = QName.createQName(MED_MODEL_PREFIX, "cod_num_caja");
	final public static QName PROP_SEC_NUMBER = QName.createQName(MED_MODEL_PREFIX, "sec_number");
	final public static QName PROP_PAGE_NUMBER= QName.createQName(MED_MODEL_PREFIX, "page_number");
	final public static QName PROP_LOTE_NUMBER = QName.createQName(MED_MODEL_PREFIX, "lote_number");
	final public static QName PROP_FISCAL_YEAR = QName.createQName(MED_MODEL_PREFIX, "fiscal_year");
	final public static QName PROP_SUPPLEMENT = QName.createQName(MED_MODEL_PREFIX, "supplement");
}
