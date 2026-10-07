package cl.bancoxyz.batch.reader;

import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;

import cl.bancoxyz.batch.model.RegistroCsv;

/**
 * LineMapper que ademas guarda el numero de linea y el texto original en cada registro.
 * El tokenizer no es estricto: lineas con columnas faltantes se completan con vacio y
 * el processor decide si el registro es valido.
 */
public class LineaAwareLineMapper<T extends RegistroCsv> implements LineMapper<T> {

    private final DefaultLineMapper<T> delegate = new DefaultLineMapper<>();

    public LineaAwareLineMapper(String[] columnas, FieldSetMapper<T> mapper) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(",");
        tokenizer.setNames(columnas);
        tokenizer.setStrict(false);
        delegate.setLineTokenizer(tokenizer);
        delegate.setFieldSetMapper(mapper);
    }

    @Override
    public T mapLine(String line, int lineNumber) throws Exception {
        T item = delegate.mapLine(line, lineNumber);
        item.setLinea(lineNumber);
        item.setRaw(line.length() > 500 ? line.substring(0, 500) : line);
        return item;
    }
}
