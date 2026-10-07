package cl.bancoxyz.batch.reader;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import cl.bancoxyz.batch.config.BatchProperties;
import cl.bancoxyz.batch.model.RegistroCsv;

/**
 * Fabrica de lectores CSV thread-safe para steps multihilo.
 * FlatFileItemReader no es thread-safe, por eso se envuelve en SynchronizedItemStreamReader
 * (la lectura es secuencial, pero el procesamiento y la escritura ocurren en paralelo).
 */
@Component
public class CsvReaders {

    private final ResourceLoader resourceLoader;
    private final BatchProperties props;

    public CsvReaders(ResourceLoader resourceLoader, BatchProperties props) {
        this.resourceLoader = resourceLoader;
        this.props = props;
    }

    public Resource recurso(String semana, String archivo) {
        return resourceLoader.getResource(props.inputDir() + "/" + semana + "/" + archivo);
    }

    public <T extends RegistroCsv> SynchronizedItemStreamReader<T> crear(String nombre, String semana, String archivo,
                                                                         String[] columnas, FieldSetMapper<T> mapper) {
        FlatFileItemReader<T> reader = new FlatFileItemReader<>();
        reader.setName(nombre);
        reader.setResource(recurso(semana, archivo));
        reader.setLinesToSkip(1);           // cabecera
        reader.setStrict(true);              // si el archivo no existe, el step falla (fallo critico)
        reader.setEncoding("UTF-8");
        reader.setSaveState(false);          // obligatorio en steps multihilo
        reader.setLineMapper(new LineaAwareLineMapper<>(columnas, mapper));

        SynchronizedItemStreamReader<T> sincronizado = new SynchronizedItemStreamReader<>();
        sincronizado.setDelegate(reader);
        return sincronizado;
    }
}
