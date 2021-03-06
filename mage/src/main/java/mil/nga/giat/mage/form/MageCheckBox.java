package mil.nga.giat.mage.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.CheckBox;

import java.io.Serializable;

import mil.nga.giat.mage.R;

public class MageCheckBox extends CheckBox implements MageControl {

	private String propertyKey;
	private MagePropertyType propertyType;
	protected Boolean isRequired = Boolean.FALSE;

	public MageCheckBox(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MageFormElement);
		setPropertyKey(typedArray.getString(R.styleable.MageFormElement_propertyKey));
		setPropertyType(MagePropertyType.getPropertyType(typedArray.getInt(R.styleable.MageFormElement_propertyType, 0)));
		typedArray.recycle();
	}

	@Override
	public void setPropertyKey(String propertyKey) {
		this.propertyKey = propertyKey;
	}

	@Override
	public String getPropertyKey() {
		return this.propertyKey;
	}

	@Override
	public void setPropertyType(MagePropertyType propertyType) {
		this.propertyType = propertyType;
	}

	@Override
	public MagePropertyType getPropertyType() {
		return this.propertyType;
	}

	@Override
	public Boolean getPropertyValue() {
		return isChecked();
	}

	@Override
	public Boolean isRequired() {
		return isRequired;
	}

	@Override
	public void setRequired(Boolean isRequired) {
		this.isRequired = isRequired;
	}

	@Override
	public void setPropertyValue(Serializable value) {
		if(value == null) {
			return;
		}
		if (value instanceof Boolean) {
			setChecked((Boolean) value);
		} else if (value instanceof String) {
			setChecked(Boolean.valueOf((String) value));
		}
	}

	@Override
	public CharSequence getError() {
		return super.getError();
	}
}
