import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws SQLException {
        SwingUtilities.invokeLater(() -> {
            try {
                new InterfazUsuario();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}

class InterfazUsuario implements ActionListener {
    private JFrame frame;
    private JComboBox<String> opcionesMenu;
    private JButton botonEjecutar;
    private JTextArea textAreaResultado;
    private TareaDAO tareaDAO;

    public InterfazUsuario() throws SQLException {
        frame = new JFrame("TaskList");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        tareaDAO = new TareaDAO(Conexion.obtenerConexion());

        
        String[] opciones = {"Agregar nueva tarea", "Marcar tarea como completada", "Eliminar tarea",
                             "Generar reporte de tareas en curso", "Generar reporte de tareas completadas"};
        opcionesMenu = new JComboBox<>(opciones);
        frame.add(opcionesMenu, BorderLayout.NORTH);

        
        botonEjecutar = new JButton("Aceptar");
        botonEjecutar.addActionListener(this);
        frame.add(botonEjecutar, BorderLayout.CENTER);

        
        textAreaResultado = new JTextArea(10, 40);
        textAreaResultado.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textAreaResultado);
        frame.add(scrollPane, BorderLayout.SOUTH);

        
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String opcionSeleccionada = (String) opcionesMenu.getSelectedItem();
        switch (opcionSeleccionada) {
            case "Agregar nueva tarea":
                mostrarDialogoAgregarTarea();
                break;
            case "Marcar tarea como completada":
                mostrarVentanaDetalles("Marcar tarea como completada");
                break;
            case "Eliminar tarea":
                mostrarVentanaDetalles("Eliminar tarea");
                break;
            case "Generar reporte de tareas en curso":
                try {
                    generarReporteTareas("Pendiente");
                } catch (SQLException ex) {
                    mostrarError("Error al generar el reporte: " + ex.getMessage());
                }
                break;
            case "Generar reporte de tareas completadas":
                try {
                    generarReporteTareas("Completada");
                } catch (SQLException ex) {
                    mostrarError("Error al generar el reporte: " + ex.getMessage());
                }
                break;
        }
    }

    private void mostrarVentanaDetalles(String accion) {
        try {
            List<Tarea> todasLasTareas = tareaDAO.obtenerTareas();

            if (todasLasTareas.isEmpty()) {
                mostrarMensaje("No hay tareas disponibles.");
                return;
            }

            // Filtrar tareas según su estado
            List<Tarea> tareasDisponibles = new ArrayList<>();
            for (Tarea tarea : todasLasTareas) {
                if (!(tarea.getEstado().equals("Completada") && accion.equals("Marcar tarea como completada"))
                        && !(tarea.getEstado().equals("Eliminada") && accion.equals("Eliminar tarea"))) {
                    tareasDisponibles.add(tarea);
                }
            }

            if (tareasDisponibles.isEmpty()) {
                mostrarMensaje("No hay tareas disponibles para esta acción.");
                return;
            }

            // Crear lista de IDs de tareas
            List<String> idsTareas = new ArrayList<>();
            for (Tarea tarea : tareasDisponibles) {
                idsTareas.add(String.valueOf(tarea.getId()));
            }

            JComboBox<String> tareasComboBox = new JComboBox<>(idsTareas.toArray(new String[0]));

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("Seleccione la tarea:"));
            panel.add(tareasComboBox);

            int resultado = JOptionPane.showConfirmDialog(null, panel, accion,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (resultado == JOptionPane.OK_OPTION) {
                //Obtener el ID de la tarea seleccionada
                String idTareaSeleccionada = (String) tareasComboBox.getSelectedItem();
                //Buscar la tarea correspondiente al ID
                Tarea tareaSeleccionada = null;
                for (Tarea tarea : tareasDisponibles) {
                    if (String.valueOf(tarea.getId()).equals(idTareaSeleccionada)) {
                        tareaSeleccionada = tarea;
                        break;
                    }
                }
                if (tareaSeleccionada != null) {
                    mostrarVentanaDetallesTarea(tareaSeleccionada, accion);
                } else {
                    mostrarError("Seleccione una tarea válida.");
                }
            }
        } catch (SQLException ex) {
            mostrarError("Error al obtener las tareas: " + ex.getMessage());
        }
    }


    private void mostrarVentanaDetallesTarea(Tarea tarea, String accion) {
        JFrame ventanaDetalles = new JFrame("Detalles de la tarea");
        ventanaDetalles.setLayout(new BorderLayout());

        JPanel panelDetalles = new JPanel(new GridLayout(0, 1));
        panelDetalles.add(new JLabel("ID: " + tarea.getId()));
        panelDetalles.add(new JLabel("Nombre: " + tarea.getNombre()));
        panelDetalles.add(new JLabel("Descripción: " + tarea.getDescripcion()));
        panelDetalles.add(new JLabel("Estado: " + tarea.getEstado()));

        ventanaDetalles.add(panelDetalles, BorderLayout.CENTER);

        JButton botonConfirmar = new JButton(accion);
        botonConfirmar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (accion.equals("Marcar tarea como completada")) {
                        tareaDAO.marcarTareaComoCompletada(tarea.getId());
                        mostrarMensaje("Tarea marcada como completada correctamente.");
                    } else if (accion.equals("Eliminar tarea")) {
                        tareaDAO.eliminarTarea(tarea.getId());
                        mostrarMensaje("Tarea eliminada correctamente.");
                    }
                    ventanaDetalles.dispose();
                } catch (SQLException ex) {
                    mostrarError("Error al realizar la acción: " + ex.getMessage());
                }
            }
        });
        ventanaDetalles.add(botonConfirmar, BorderLayout.SOUTH);

        ventanaDetalles.pack();
        ventanaDetalles.setLocationRelativeTo(null); 
        ventanaDetalles.setVisible(true);
    }

    private void generarReporteTareas(String estado) throws SQLException {
        List<Tarea> tareas = null;
        if (estado.equals("Pendiente") || estado.equals("En progreso")) {
            tareas = tareaDAO.obtenerTareasPorEstado("Pendiente");
            tareas.addAll(tareaDAO.obtenerTareasPorEstado("En progreso"));
        } else {
            tareas = tareaDAO.obtenerTareasPorEstado(estado);
        }

        if (tareas.isEmpty()) {
            mostrarMensaje("No hay tareas en este estado.");
        } else {
            StringBuilder reporte = new StringBuilder("Reporte de tareas " + estado + ":\n");
            for (Tarea tarea : tareas) {
                reporte.append("ID: ").append(tarea.getId()).append("\n");
                reporte.append("Nombre: ").append(tarea.getNombre()).append("\n");
                reporte.append("Descripción: ").append(tarea.getDescripcion()).append("\n");
                reporte.append("Estado: ").append(tarea.getEstado()).append("\n");
                reporte.append("-------------------------\n");
            }
            textAreaResultado.setText(reporte.toString());
        }
    }


    private void mostrarDialogoAgregarTarea() {
        JTextField nombreField = new JTextField();
        JTextField descripcionField = new JTextField();
        JComboBox<String> estadoComboBox = new JComboBox<>(new String[]{"Pendiente", "En progreso", "Completada"});

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Nombre de la tarea:"));
        panel.add(nombreField);
        panel.add(new JLabel("Descripción de la tarea:"));
        panel.add(descripcionField);
        panel.add(new JLabel("Estado de la tarea:"));
        panel.add(estadoComboBox);

        int resultado = JOptionPane.showConfirmDialog(null, panel, "Agregar nueva tarea",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resultado == JOptionPane.OK_OPTION) {
            try {
                String nombre = nombreField.getText();
                String descripcion = descripcionField.getText();
                String estado = (String) estadoComboBox.getSelectedItem();
                Tarea nuevaTarea = new Tarea(0, nombre, descripcion, estado); // ID 0 para que se genere automáticamente en la base de datos
                tareaDAO.agregarTarea(nuevaTarea);
                mostrarMensaje("Tarea agregada correctamente.");
            } catch (SQLException ex) {
                mostrarError("Error al agregar la tarea: " + ex.getMessage());
            }
        }
        
    }

    private void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(frame, mensaje);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(frame, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
