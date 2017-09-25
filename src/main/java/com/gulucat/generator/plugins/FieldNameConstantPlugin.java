package com.gulucat.generator.plugins;

import java.util.ArrayList;
import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.InnerClass;
import org.mybatis.generator.api.dom.java.InnerEnum;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * 生成常量字段插件<br>
 * 例如有个id字段，则生成一个常量 public static String ID = "id";<br>
 * <br>
 * <br>
 * 插件可配置参数：<br>
 * 
 * constantType： 可选值为"field", "innerClass", "enum"<br>
 * 可选值说明：<br>
 * "Field": 直接生成常量字段<br>
 * "InnerClass": 生成内部类的形式，常量字段被内部类包裹，类名通过fieldName指定<br>
 * "Enum": 生成枚举形式，常量字段被枚举包裹，枚举名通过fieldName指定<br>
 * 
 * fieldName：
 * 当constantType设置为innerClass或者enum时，innerClass或者enum会使用这个参数当作自身名字<br>
 * 
 * prefix： 常量字段前缀<br>
 * 
 * suffix： 常量字段后缀<br>
 * 
 * @author ljskr
 *
 */
public class FieldNameConstantPlugin extends PluginAdapter {

	/**
	 * 常量类型说明<br>
	 * Field: 直接生成常量字段<br>
	 * InnerClass: 生成内部类的形式，常量字段被内部类包裹，类名通过fieldName指定<br>
	 * Enum: 生成枚举形式，常量字段被枚举包裹，枚举名通过fieldName指定<br>
	 */
	protected enum ConstantType {
		Field, InnerClass, Enum;
	}

	private static final String DEFAULT_FIELD_NAME = "Field";

	private ConstantType type;

	private String fieldName; // when type set to InnerClass and Enum, the
								// innerClass or enum will use this name.

	private String prefix;
	private String suffix;

	private List<Field> constantFieldList = new ArrayList<Field>();
	private List<String> constantEnumList = new ArrayList<String>();
	private StringBuilder sb = new StringBuilder();

	public FieldNameConstantPlugin() {
		super();
		type = ConstantType.Field;
		constantFieldList.clear();
		constantEnumList.clear();
		fieldName = DEFAULT_FIELD_NAME;
	}

	@Override
	public boolean validate(List<String> warnings) {

		String constantType = this.properties.getProperty("constantType");
		if (constantType != null) {
			if (constantType.equalsIgnoreCase("field")) {
				this.type = ConstantType.Field;
			} else if (constantType.equalsIgnoreCase("innerClass")) {
				this.type = ConstantType.InnerClass;
			} else if (constantType.equalsIgnoreCase("enum")) {
				this.type = ConstantType.Enum;
			} else {
				warnings.add(
						"[FieldNameConstantPlugin]UNKOWN_VALUE_ERROR: this plugin is disabled. Because property constantType has set to an unkown value \""
								+ constantType
								+ "\". It must be one value of them: \"field\", \"innerClass\", \"enum\"!");
				return false;
			}
		}

		String name = this.properties.getProperty("fieldName");
		if (name != null && name.length() > 0) {
			this.fieldName = name;
		}

		String prefixString = this.properties.getProperty("prefix");
		if (prefixString != null && prefixString.length() > 0) {
			this.prefix = prefixString;
		}

		String suffixString = this.properties.getProperty("suffix");
		if (suffixString != null && suffixString.length() > 0) {
			this.suffix = suffixString;
		}

		return true;
	}

	@Override
	public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
			IntrospectedTable introspectedTable, ModelClassType modelClassType) {

		// Collect field info.
		switch (type) {
		case Enum:
			generateConstantEnum(field, topLevelClass, introspectedColumn, introspectedTable, modelClassType);
			break;

		case InnerClass:
		case Field:
		default:
			generateConstantField(field, topLevelClass, introspectedColumn, introspectedTable, modelClassType);
			break;
		}

		return true;
	}

	@Override
	public boolean modelPrimaryKeyClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		addConstantToTopLevelClass(topLevelClass);
		return true;
	}

	@Override
	public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		addConstantToTopLevelClass(topLevelClass);
		return true;
	}

	@Override
	public boolean modelRecordWithBLOBsClassGenerated(TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addConstantToTopLevelClass(topLevelClass);
		return true;
	}

	protected void addConstantToTopLevelClass(TopLevelClass topLevelClass) {
		// After all field are generated, add them to the model so that we can
		// put them together.
		switch (type) {
		case Enum:
			addConstantEnums(topLevelClass);
			break;

		case InnerClass:
			addConstantInnerClass(topLevelClass);
			break;

		case Field:
		default:
			addConstantFields(topLevelClass);
			break;
		}
	}

	/**
	 * Generate constant as public field<br>
	 * 生成常量字段
	 */
	protected void generateConstantField(Field field, TopLevelClass topLevelClass,
			IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
		Field constantField = new Field();
		constantField.setVisibility(JavaVisibility.PUBLIC);
		constantField.setStatic(true);
		constantField.setFinal(true);
		constantField.setType(new FullyQualifiedJavaType("String"));
		constantField.setName(concateString(introspectedColumn.getActualColumnName().toUpperCase()));
		constantField.setInitializationString("\"" + field.getName() + "\"");
		// Not add this field to model now. Add them after all field generated.
		// 先放到一个list中，后续统一加进class里。
		this.constantFieldList.add(constantField);
	}

	/**
	 * Add constant as public field<br>
	 * 往class中添加常量字段
	 */
	protected void addConstantFields(TopLevelClass topLevelClass) {
		for (Field field : this.constantFieldList) {
			topLevelClass.addField(field);
		}

		// Clear field list for next model
		this.constantFieldList.clear();
	}

	/**
	 * Add constant as inner class<br>
	 * 往内部类中添加常量字段
	 */
	protected void addConstantInnerClass(TopLevelClass topLevelClass) {
		InnerClass innerClass = new InnerClass(new FullyQualifiedJavaType(this.fieldName));
		innerClass.setVisibility(JavaVisibility.PUBLIC);
		innerClass.setStatic(true);

		for (Field field : this.constantFieldList) {
			innerClass.addField(field);
		}
		topLevelClass.addInnerClass(innerClass);

		// Clear field list for next model
		this.constantFieldList.clear();
	}

	/**
	 * Generate constant as enum<br>
	 * 生成枚举常量
	 */
	protected void generateConstantEnum(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
			IntrospectedTable introspectedTable, ModelClassType modelClassType) {
		this.constantEnumList.add(field.getName());
	}

	/**
	 * Add constant as enum<br>
	 * 往枚举中添加常量字段
	 */
	protected void addConstantEnums(TopLevelClass topLevelClass) {
		InnerEnum innerEnum = new InnerEnum(new FullyQualifiedJavaType(this.fieldName));
		innerEnum.setVisibility(JavaVisibility.PUBLIC);
		innerEnum.setStatic(true);

		for (String enumString : this.constantEnumList) {
			innerEnum.addEnumConstant(enumString);
		}
		topLevelClass.addInnerEnum(innerEnum);

		// Clear field list for next model
		this.constantEnumList.clear();
	}

	/**
	 * concate input string with prefix and subfix<br>
	 * 给字段名称添加前缀、后缀
	 */
	private String concateString(String input) {
		sb.setLength(0);
		if (this.prefix != null) {
			sb.append(this.prefix);
		}
		sb.append(input);
		if (this.suffix != null) {
			sb.append(this.suffix);
		}
		return sb.toString();
	}
}