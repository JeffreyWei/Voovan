package org.voovan.db.sqlsegment;

import org.voovan.db.SQLBuilder;

public class SelectColumns extends Columns {
	public SelectColumns(SQLBuilder builder) {
		super(builder);
	}
	
	public SelectColumns column(String column){
		columns.add(column);
		return this;
	}
	
	public From from() {
		builder.setFrom(new From(builder));
		return builder.getFrom();
	}
}