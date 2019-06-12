package org.adaptlab.chpir.android.survey.relations;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import org.adaptlab.chpir.android.survey.entities.Instrument;
import org.adaptlab.chpir.android.survey.entities.Response;
import org.adaptlab.chpir.android.survey.entities.Survey;

import java.util.List;

public class ProjectSurveyRelation {
    @Embedded
    public Survey survey;
    @Relation(parentColumn = "UUID", entityColumn = "SurveyUUID", entity = Response.class)
    public List<Response> responses;
    @Relation(parentColumn = "InstrumentRemoteId", entityColumn = "RemoteId", entity = Instrument.class)
    public List<Instrument> instruments;
}
